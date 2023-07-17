package services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dto.MessageType;
import dto.PlayerCommand;
import dto.ServerGeneralMessage;
import exceptions.MatchTerminationException;
import exceptions.PlayerConnectionException;
import model.player.MatchPlayerEntity;
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
    protected final List<MatchPlayerEntity> m_MatchPlayerEntities;
    protected final List<MatchPlayerEntity> m_MatchQuitedMatchPlayerEntities;
    protected final List<MatchPlayerEntity> m_WaitingToQuit;
    protected MatchScoreManager m_MatchScore;
    protected boolean m_IsGameOver;

    protected MatchService(String i_MatchIdentifier, List<MatchPlayerEntity> i_MatchPlayersList)
    {
        this.m_MatchIdentifier = i_MatchIdentifier;
        this.m_MatchPlayerEntities = i_MatchPlayersList;
        this.m_MatchScore = new MatchScoreManager();
        this.m_MatchQuitedMatchPlayerEntities = new ArrayList<>();
        this.m_WaitingToQuit = new ArrayList<>();
        this.m_IsGameOver = false;
    }

    // Abstract methods.
    abstract public void run();
    abstract public void removeWaitingToQuitPlayers() throws Exception;
    abstract public void SendPlayerCommand(PlayerCommand i_PlayerCommand);
    abstract public void EndMatch(String i_MatchEndedReason);

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

    protected MatchPlayerEntity findPlayerInList(String i_PlayerUsername)
    {
        Optional<MatchPlayerEntity> playerOptional = this.m_MatchPlayerEntities.stream()
                .filter(p -> p.EqualByUsername(i_PlayerUsername))
                .findFirst();

        return playerOptional.orElse(null);
    }

    protected void updateCoinsOfPlayer(String i_PlayerUsername)
    {
        MatchPlayerEntity matchPlayer = findPlayerInList(i_PlayerUsername);

        if(matchPlayer != null)
        {
            matchPlayer.CoinCollected();
            LoggerManager.trace(i_PlayerUsername + " Collected a coin!");
        }
    }

    protected void handlePlayerResponse(MatchPlayerEntity i_Match_PlayerEntity, PlayerCommand i_PlayerCommand) throws PlayerConnectionException {

        switch (i_PlayerCommand.GetAction())
        {
            case IDLE:
            case RUN_RIGHT:
            case RUN_LEFT:
            case DEATH:
            case UPDATE_LOCATION:
            case JUMP: {
                i_Match_PlayerEntity.UpdateLocation(i_PlayerCommand.GetLocation());
                this.SendPlayerCommand(i_PlayerCommand);
                break;
            }
            case COIN_COLLECT:
            {
                updateCoinsOfPlayer(i_PlayerCommand.GetUsername());
                break;
            }
            case COMPLETE_LEVEL: {

                try
                {
                    i_Match_PlayerEntity.MarkAsFinish();
                    int playerScorePosition = this.m_MatchScore.SetPlayerScore(i_Match_PlayerEntity);

                    // Send score position to player.
                    ServerGeneralMessage scorePositionAnnouncement
                            = new ServerGeneralMessage(ServerGeneralMessage.eActionType.COMPLETE_MATCH, "Finished #"+playerScorePosition + " place!");
                    i_Match_PlayerEntity.SendMessage(scorePositionAnnouncement.toString());
                }
                catch(IllegalArgumentException iae)
                {
                    LoggerManager.warning(i_Match_PlayerEntity.GetUserName() + " " + iae.getMessage());
                } catch (SocketTimeoutException e) {
                    throw new PlayerConnectionException(GlobalSettings.CLIENT_CLOSED_CONNECTION);
                }

                break;
            }
            case QUIT:
            {
                throw new PlayerConnectionException(GlobalSettings.CLIENT_CLOSED_CONNECTION);
            }
            default:
            {
                LoggerManager.warning("Player " + i_PlayerCommand.GetUsername() + " command not found");
            }
        }

    }

    // Implementation of shared  methods.
    protected void initMatch() throws Exception {

        this.waitForPlayersToBeReady();
        MatchLogger.Debug(GetMatchIdentifier(), "Players ready.");

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, "Starting match..").toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Start message sent.");

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.ACTION, "START").toString());
        MatchLogger.Info(GetMatchIdentifier(), "Starting game.");
    }

    protected void actionOnMatchPlayers(Consumer<MatchPlayerEntity> processor) {

        for (MatchPlayerEntity matchEntity : m_MatchPlayerEntities) {
            if (matchEntity.IsConnectionAlive()) {
                try {
                    processor.accept(matchEntity);
                } catch (Exception e) {
                    LoggerManager.error("Player " + matchEntity.GetUserName() + " " + e.getMessage());
                }
            }
        }
    }

    protected void waitForPlayersToBeReady() throws Exception {

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
        for(MatchPlayerEntity matchPlayer : m_MatchPlayerEntities)
        {
            if(!matchPlayer.IsFinishedMatch())
                return false;
        }

        return true;
    }

    protected int getNumOfPlayerInMatch()
    {
        return this.m_MatchPlayerEntities.size();
    }

    public synchronized void RemovePlayerFromMatch(MatchPlayerEntity i_MatchPlayer)
    {
        this.m_WaitingToQuit.add(i_MatchPlayer);
    }

    public boolean IsGameOver() {
        return this.m_IsGameOver;
    }
}
