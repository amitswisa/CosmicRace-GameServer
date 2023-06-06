package match_making;

import exceptions.MatchTerminationException;
import exceptions.PlayerConnectionException;
import player.Location;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import dto.MessageType;
import dto.PlayerCommand;
import utils.json.JsonFormatter;
import player.Player;
import com.google.gson.JsonObject;
import dto.ServerGeneralMessage;
import interfaces.Match;
import utils.GlobalSettings;
import utils.loggers.MatchLogger;
import utils.loggers.LoggerManager;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static dto.PlayerAction.*;

final class OnlineMatch extends Thread implements Match {

    private final List<Player> m_MatchPlayers;
    private final List<Player> m_MatchQuitedPlayers;
    private final List<Player> m_WaitingToQuit;
    private final String m_MatchIdentifier;
    private MatchScoreManager m_MatchScore;
    private boolean m_IsGameOver;

    public OnlineMatch(String i_MatchIdentifier, List<Player> i_MatchPlayers) {

        this.m_MatchIdentifier = i_MatchIdentifier;
        this.m_MatchPlayers = i_MatchPlayers;
        this.m_MatchScore = new MatchScoreManager();
        this.m_MatchQuitedPlayers = new ArrayList<>(getNumOfPlayerInMatch());
        this.m_WaitingToQuit = new ArrayList<>(getNumOfPlayerInMatch());
        this.m_IsGameOver = false;
        this.actionOnMatchPlayers(player -> player.SetMatch(this));

        SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, "Players found, creating a match.").toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Trying to create a match!");

        SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.DATA, getMatchPlayersAsJson()).toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Players initial data sent!");
    }

    private void initMatch() throws Exception {

        this.waitForPlayersToBeReady();
        MatchLogger.Debug(GetMatchIdentifier(), "Players ready.");

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, "Starting match..").toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Start message sent.");

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.ACTION, "START").toString());
        MatchLogger.Info(GetMatchIdentifier(), "Starting game.");
    }

    @Override
    public void run() {

        PlayerCommand playerCommand = new PlayerCommand();

        try
        {
            this.initMatch();

            while (!isMatchOver())
            {
                for(Player player : m_MatchPlayers)
                {
                    try {
                        String playerUpdate = player.ReadMessage();

                        if(!playerUpdate.equals(GlobalSettings.NO_MESSAGES_IN_CLIENT_BUFFER)) {
                            LoggerManager.info(playerUpdate);
                            playerCommand.ParseFromJson(playerUpdate);
                            this.handlePlayerResponse(player, playerCommand);
                        }
                    }
                    catch(PlayerConnectionException pqe)
                    {
                        player.CloseConnection(pqe.getMessage());
                    }
                    catch(JsonSyntaxException jse)
                    {
                        MatchLogger.Error(this.GetMatchIdentifier()
                                , "Player " + player.GetUserName() + " command error: " + jse.getMessage());
                    }
                }

                this.removeWaitingToQuitPlayers();
            }

            this.EndMatch(GlobalSettings.MATCH_ENDED);

        } catch(Exception e){
            this.EndMatch(e.getMessage());
        }
    }

    private void handlePlayerResponse(Player i_Player, PlayerCommand i_PlayerCommand) throws PlayerConnectionException {

        switch (i_PlayerCommand.GetAction())
        {
            case IDLE:
            case RUN_RIGHT:
            case RUN_LEFT:
            case DEATH:
            case UPDATE_LOCATION:
            case JUMP: {
                i_Player.UpdateLocation(i_PlayerCommand.GetLocation());
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
                    i_Player.MarkAsFinish();
                    int playerScorePosition = this.m_MatchScore.SetPlayerScore(i_Player);

                    // Send score position to player.
                    ServerGeneralMessage scorePositionAnnouncement
                            = new ServerGeneralMessage(ServerGeneralMessage.eActionType.COMPLETE_MATCH, "Finished #"+playerScorePosition + " place!");
                    i_Player.SendMessage(scorePositionAnnouncement.toString());
                }
                catch(IllegalArgumentException iae)
                {
                    LoggerManager.warning(i_Player.GetUserName() + " " + iae.getMessage());
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

    private void updateCoinsOfPlayer(String i_PlayerUsername)
    {
        Player playerToUpdate = findPlayerInList(i_PlayerUsername);

        if(playerToUpdate != null)
        {
            playerToUpdate.CoinCollected();
            LoggerManager.trace(i_PlayerUsername + " Collected a coin!");
        }
    }

    private Player findPlayerInList(String i_PlayerUsername)
    {
        Optional<Player> playerOptional = this.m_MatchPlayers.stream()
                .filter(p -> p.EqualByUsername(i_PlayerUsername))
                .findFirst();

        return playerOptional.orElse(null);
    }

    private boolean isMatchOver()
    {
        return (m_IsGameOver || this.isActivePlayersFinished());
    }

    private boolean isActivePlayersFinished()
    {
        for(Player player : m_MatchPlayers)
        {
            if(!player.IsFinishedMatch())
                return false;
        }

        return true;
    }

    @Override
    public synchronized void RemovePlayerFromMatch(Player player)
    {
        this.m_WaitingToQuit.add(player);
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

    public void SendPlayerCommand(PlayerCommand i_PlayerCommand)
    {
        actionOnMatchPlayers((player) -> {

            if(!player.GetUserName().equals(i_PlayerCommand.GetUsername()))
            {
                try {
                    String command = JsonFormatter.GetGson().toJson(i_PlayerCommand, PlayerCommand.class);

                    player.SendMessage(command);
                } catch(SocketTimeoutException ste) {
                    player.CloseConnection(ste.getMessage());
                }
            }
        });

        LoggerManager.trace(i_PlayerCommand.toString());
    }

    public String GetMatchIdentifier() {
        return this.m_MatchIdentifier;
    }

    private void removeWaitingToQuitPlayers() throws Exception
    {

        if(this.m_WaitingToQuit.size() > 0)
        {
            this.m_MatchPlayers.removeAll(this.m_WaitingToQuit);
            this.m_MatchQuitedPlayers.addAll(this.m_WaitingToQuit);

            this.m_WaitingToQuit.forEach((quitedPlayer) -> {

                this.SendPlayerCommand(new PlayerCommand(MessageType.COMMAND,
                        quitedPlayer.GetUserName(), RIVAL_QUIT, new Location(0,0)));

                MatchLogger.Debug(GetMatchIdentifier(), "Player " + quitedPlayer.GetUserName() + " disconnected.");
            });

            this.m_WaitingToQuit.clear();

            if (!this.m_IsGameOver
                    && this.m_MatchPlayers.size() < GlobalSettings.MINIMUM_AMOUNT_OF_PLAYERS)
            {
                throw new MatchTerminationException(this.GetMatchIdentifier(), GlobalSettings.NOT_ENOUGH_PLAYERS_TO_CONTINUE);
            }
        }
    }

    private void waitForPlayersToBeReady() throws Exception {

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

    private void actionOnMatchPlayers(Consumer<Player> processor) {

        for (Player player : m_MatchPlayers) {
            if (player.IsConnectionAlive()) {
                try {
                    processor.accept(player);
                } catch (Exception e) {
                    LoggerManager.error("Player " + player.GetUserName() + " " + e.getMessage());
                }
            }
        }
    }

    private String getMatchPlayersAsJson()
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

    @Override
    public boolean IsGameOver() {
        return this.m_IsGameOver;
    }

    private int getNumOfPlayerInMatch()
    {
        return this.m_MatchPlayers.size();
    }

    @Override
    public void EndMatch(String i_MatchEndedReason) {

        this.m_IsGameOver = true;

        if(!i_MatchEndedReason.equals(GlobalSettings.MATCH_ENDED))
        {
            MatchLogger.Error(GetMatchIdentifier(), i_MatchEndedReason);
        }
        else
        {
            MatchLogger.Info(GetMatchIdentifier(), i_MatchEndedReason);
        }

        // TODO - update players coins and stats on database.
        //  /30.4/UPDATE - only stats left.
        //this.actionOnMatchPlayers(p -> DBHandler.updateStatsInDB(p.GetCharacter()));

        ServerGeneralMessage finalMatchEndedMessage
                = new ServerGeneralMessage(ServerGeneralMessage.eActionType.MATCH_TERMINATION, i_MatchEndedReason);

        this.actionOnMatchPlayers((player) -> {
            try {
                player.SendMessage(finalMatchEndedMessage.toString());
            } catch (SocketTimeoutException e) {
                MatchLogger.Warning(GetMatchIdentifier()
                        , "Couldn't update player " + player.GetUserName() + " on match ending.");
            }
        });

        MatchMaking.RemoveActiveMatch(this);
        this.interrupt();
    }
}