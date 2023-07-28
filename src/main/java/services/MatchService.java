package services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dto.MessageType;
import dto.PlayerCommand;
import dto.ServerGeneralMessage;
import entities.player.HostEntity;
import exceptions.MatchTerminationException;
import exceptions.PlayerConnectionException;
import model.player.PlayerEntity;
import utils.GlobalSettings;
import utils.json.JsonFormatter;
import utils.loggers.LoggerManager;
import utils.loggers.MatchLogger;
import utils.match.MatchScoreManager;
import utils.player.Location;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static dto.PlayerAction.*;

public abstract class MatchService extends Thread
{
    protected final String m_MatchIdentifier;
    protected final List<PlayerEntity> m_MatchPlayerEntities;
    protected final List<PlayerEntity> m_MatchQuitedMatchPlayerEntities;
    protected final List<PlayerEntity> m_WaitingToQuit;
    protected MatchScoreManager m_MatchScore;
    protected boolean m_IsGameOver;

    protected boolean m_IsGameStarted;

    protected MatchService(String i_MatchIdentifier, List<PlayerEntity> i_MatchPlayersList)
    {
        this.m_MatchIdentifier = i_MatchIdentifier;
        this.m_MatchPlayerEntities = i_MatchPlayersList;
        this.m_MatchScore = new MatchScoreManager();
        this.m_MatchQuitedMatchPlayerEntities = new ArrayList<>();
        this.m_WaitingToQuit = new ArrayList<>();
        this.m_IsGameOver = false;
        this.m_IsGameStarted = false;
    }

    // Abstract methods.
    abstract public void run();
    abstract public void SendPlayerCommand(PlayerCommand i_PlayerCommand);
    abstract public void EndMatch(String i_MatchEndedReason);
    abstract protected void initMatch() throws Exception;
    abstract protected void actionOnMatchPlayers(Consumer<PlayerEntity> processor);
    abstract public PlayerEntity GetHost();

    protected String getMatchPlayersAsJson()
    {
        Map<String, JsonElement> playersMap = new HashMap<>();

        this.actionOnMatchPlayers((player) -> {
            JsonElement playerData = player.GetPlayerMatchData();
            playersMap.put(player.GetUserName(), playerData);
        });

        JsonObject mainObject = new JsonObject();
        mainObject.addProperty("MatchIdentifier", this.GetMatchIdentifier());

        JsonElement playersElement = JsonFormatter.GetGson().toJsonTree(playersMap);
        mainObject.add("Players", playersElement);

        return JsonFormatter.GetGson().toJson(mainObject);
    }

    protected PlayerEntity findPlayerInList(String i_PlayerUsername)
    {
        Optional<PlayerEntity> playerOptional = this.m_MatchPlayerEntities.stream()
                .filter(p -> p.EqualByUsername(i_PlayerUsername))
                .findFirst();

        return playerOptional.orElse(null);
    }

    protected void updateCoinsOfPlayer(String i_PlayerUsername)
    {
        PlayerEntity matchPlayer = findPlayerInList(i_PlayerUsername);

        if(matchPlayer != null)
        {
            matchPlayer.CoinCollected();
            LoggerManager.trace(i_PlayerUsername + " Collected a coin!");
        }
    }

    protected void handlePlayerResponse(PlayerEntity i_Match_PlayerEntity, PlayerCommand i_PlayerCommand) throws PlayerConnectionException
    {

        switch (i_PlayerCommand.GetAction()) {
            case IDLE, RUN_RIGHT, RUN_LEFT, DEATH, UPDATE_LOCATION, JUMP -> {
                i_Match_PlayerEntity.UpdateLocation(i_PlayerCommand.GetLocation());
                this.SendPlayerCommand(i_PlayerCommand);
                break;
            }
            case COIN_COLLECT -> {
                updateCoinsOfPlayer(i_PlayerCommand.GetUsername());
                break;
            }
            case COMPLETE_LEVEL -> {

                try {
                    i_Match_PlayerEntity.MarkAsFinish();
                    int playerScorePosition = this.m_MatchScore.SetPlayerScore(i_Match_PlayerEntity);

                    // Send score position to player.
                    ServerGeneralMessage scorePositionAnnouncement
                            = new ServerGeneralMessage(ServerGeneralMessage.eActionType.COMPLETE_MATCH, "Finished #" + playerScorePosition + " place!");
                    i_Match_PlayerEntity.SendMessage(scorePositionAnnouncement.toString());
                } catch (IllegalArgumentException iae) {
                    LoggerManager.warning(i_Match_PlayerEntity.GetUserName() + " " + iae.getMessage());
                } catch (SocketTimeoutException e) {
                    throw new PlayerConnectionException(GlobalSettings.CLIENT_CLOSED_CONNECTION);
                }

                break;
            }
            case QUIT -> {
                throw new PlayerConnectionException(GlobalSettings.CLIENT_CLOSED_CONNECTION);
            }
            default -> {
                LoggerManager.warning("Player " + i_PlayerCommand.GetUsername() + " command not found");
            }
        }

    }

    protected void waitForPlayersToBeReady() throws Exception
    {

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.CONFIRMATION, GlobalSettings.PLAYER_READY_MESSAGE).toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Waiting for player to be ready...");
        AtomicBoolean isEveryoneReady = new AtomicBoolean(false);

        do {
            isEveryoneReady.set(true);
            actionOnMatchPlayers((player) -> {

                try {
                    if (!player.IsReady()) {
                        String msg = player.ReadMessage();

                        if (!msg.equals(GlobalSettings.PLAYER_READY_RESPONSE_MESSAGE))
                            isEveryoneReady.set(false);
                        else {
                            player.MarkAsReady();
                            MatchLogger.Debug(GetMatchIdentifier(), "Player " + player.GetUserName() + " is ready!");
                        }

                    }
                } catch(IOException ioe) {
                    player.CloseConnection(ioe.getMessage());
                    isEveryoneReady.set(false);
                }

            });

            this.removeWaitingToQuitPlayers();

            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {
                LoggerManager.error(e.getMessage());
            }
        }
        while (!isEveryoneReady.get());
    }

    protected void removeWaitingToQuitPlayers() throws Exception
    {

        if(this.m_WaitingToQuit.size() > 0)
        {
            this.m_MatchPlayerEntities.removeAll(this.m_WaitingToQuit);
            this.m_MatchQuitedMatchPlayerEntities.addAll(this.m_WaitingToQuit);

            this.m_WaitingToQuit.forEach((quitedPlayer) -> {

                this.SendPlayerCommand(new PlayerCommand(MessageType.COMMAND,
                        quitedPlayer.GetUserName(), RIVAL_QUIT, new Location(0,0)));

                MatchLogger.Debug(GetMatchIdentifier(), "Player " + quitedPlayer.GetUserName() + " disconnected.");
            });

            this.m_WaitingToQuit.clear();

            if (!this.m_IsGameOver
                    && this.m_MatchPlayerEntities.size() < GlobalSettings.MINIMUM_AMOUNT_OF_PLAYERS
                    && this.m_IsGameStarted)
            {
                throw new MatchTerminationException(this.GetMatchIdentifier(), GlobalSettings.NOT_ENOUGH_PLAYERS_TO_CONTINUE);
            }
        }
    }

    public void SendMessageToAll(String i_Message)
    {
        actionOnMatchPlayers(player -> {

            try {
                player.SendMessage(i_Message);
            } catch(SocketTimeoutException ste) {
                player.CloseConnection(ste.getMessage());
            }

        });
    }

    public String GetMatchIdentifier() {
        return this.m_MatchIdentifier;
    }

    protected boolean isMatchOver()
    {
        return (m_IsGameOver || this.isActivePlayersFinished());
    }

    protected boolean isActivePlayersFinished()
    {
        for(PlayerEntity matchPlayer : m_MatchPlayerEntities)
        {
            if(!matchPlayer.IsFinishedMatch())
                return false;
        }

        return true;
    }

    protected void setMatchStarted()
    {
        this.m_IsGameStarted = true;
    }

    protected int getNumOfPlayerInMatch()
    {
        return this.m_MatchPlayerEntities.size();
    }

    protected void runGame() throws Exception
    {
        PlayerCommand playerCommand = new PlayerCommand();

        while (!isMatchOver())
        {
            for(PlayerEntity matchPlayer : m_MatchPlayerEntities)
            {
                try {
                    String playerUpdate = matchPlayer.ReadMessage();

                    if(!playerUpdate.equals(GlobalSettings.NO_MESSAGES_IN_CLIENT_BUFFER))
                    {
                        LoggerManager.info(playerUpdate);
                        playerCommand.ParseFromJson(playerUpdate);
                        this.handlePlayerResponse(matchPlayer, playerCommand);
                    }
                }
                catch(PlayerConnectionException pqe)
                {
                    if(matchPlayer instanceof HostEntity)
                    {
                        throw new SocketTimeoutException("Host closed the game session.");
                    }
                    else
                    {
                        matchPlayer.CloseConnection(pqe.getMessage());
                    }
                }
                catch(JsonSyntaxException jse)
                {
                    MatchLogger.Error(this.GetMatchIdentifier()
                            , "Player " + matchPlayer.GetUserName() + " command error: " + jse.getMessage());
                }
            }

            this.removeWaitingToQuitPlayers();
        }

        this.EndMatch(GlobalSettings.MATCH_ENDED);
    }

    public synchronized void RemovePlayerFromMatch(PlayerEntity i_MatchPlayer)
    {
        this.m_WaitingToQuit.add(i_MatchPlayer);
    }

    public boolean IsGameOver() {
        return this.m_IsGameOver;
    }
}
