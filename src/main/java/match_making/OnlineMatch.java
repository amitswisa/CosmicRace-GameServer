package match_making;

import com.google.gson.JsonElement;
import json.JsonFormatter;
import player.Player;
import com.google.gson.JsonObject;
import dto.ClientMessage;
import interfaces.Match;
import utils.GlobalSettings;
import utils.logs.MatchLogger;
import utils.singletons.DBHandler;
import utils.logs.LoggerManager;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

final class OnlineMatch extends Thread implements Match {

    private List<Player> m_MatchPlayers;
    private List<Player> m_MatchQuitedPlayers;
    private List<Player> m_WaitingToQuit;
    private final String m_MatchIdentifier;
    private boolean m_IsGameOver;

    public OnlineMatch(String i_MatchIdentifier, List<Player> i_MatchPlayers) {

        this.m_MatchIdentifier = i_MatchIdentifier;
        this.m_MatchPlayers = i_MatchPlayers;
        this.m_MatchQuitedPlayers = new ArrayList<>(getNumOfPlayerInMatch());
        this.m_WaitingToQuit = new ArrayList<>(getNumOfPlayerInMatch());
        this.m_IsGameOver = false;
        this.actionOnMatchPlayers(player -> player.SetMatch(this));

        SendToAll(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Players found, creating a match.").toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Trying to create a match!");

        SendToAll(new ClientMessage(ClientMessage.MessageType.DATA, getMatchPlayersAsJson()).toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Players initial data sent!");
    }

    private void initMatch() throws Exception {

        this.waitForPlayersToBeReady();
        MatchLogger.Debug(GetMatchIdentifier(), "Players ready.");

        this.SendToAll(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Starting match..").toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Start message sent.");

        this.SendToAll(new ClientMessage(ClientMessage.MessageType.ACTION, "START").toString());
        MatchLogger.Info(GetMatchIdentifier(), "Starting game.");
    }

    @Override
    public void run() {

        try
        {
            this.initMatch();

            while (!m_IsGameOver)
            {
                for(Player player : m_MatchPlayers)
                {
                    try
                    {
                        String playerResponse = player.ReadMessage();
                        this.handlePlayerResponse(player, playerResponse.trim().toUpperCase());
                    } catch(IOException ioe) {
                        player.CloseConnection(ioe.getMessage());
                    }
                }

                this.SendToAll("Players Updates!");
                this.removeWaitingToQuitPlayers();

                if(!this.matchIsOver())
                {
                    // collecting data from client each 200ms.
                    MatchLogger.Debug(this.GetMatchIdentifier(), "Thread action - Sleep 200ms.");

                    try {
                        Thread.sleep(200);
                    } catch(InterruptedException ie) {
                        MatchLogger.Error(this.GetMatchIdentifier()," has been interrupted.");
                    }
                }

            }

        } catch(Exception e){
            this.EndMatch(e.getMessage());
            return;
        }

        this.EndMatch(GlobalSettings.MATCH_ENDED);

    }

    private void handlePlayerResponse(Player i_Player, String i_PlayerResponse) throws IOException {
        switch (i_PlayerResponse) {
            case GlobalSettings.CLIENT_QUITED_BY_CHOICE: {
                throw new IOException(GlobalSettings.CLIENT_CLOSED_CONNECTION);
            }
            default: {
                LoggerManager.info("Player " + i_Player.GetUserName() + " response: " + i_PlayerResponse);
            }
        }
    }

    private boolean matchIsOver() {
        return (m_IsGameOver || this.isActivePlayersFinished());
    }

    // TODO - Create that function.
    private boolean isActivePlayersFinished() {
        return false;
    }

    @Override
    public synchronized void RemovePlayerFromMatch(Player player)
    {
        this.m_WaitingToQuit.add(player);
    }

    public void SendToAll(String message){
        actionOnMatchPlayers(player -> {

            try {
                player.SendMessage(message);
            } catch(SocketTimeoutException ste) {
                player.CloseConnection(ste.getMessage());
            }

        });
    }

    public String GetMatchIdentifier() {
        return this.m_MatchIdentifier;
    }

    private void removeWaitingToQuitPlayers() throws Exception {

        if(this.m_WaitingToQuit.size() > 0)
        {
            this.m_MatchPlayers.removeAll(this.m_WaitingToQuit);
            this.m_MatchQuitedPlayers.addAll(this.m_WaitingToQuit);

            // TODO - announce of player quited.
            this.m_WaitingToQuit.forEach((quitedPlayer) -> {
                MatchLogger.Debug(GetMatchIdentifier(), "Player " + quitedPlayer.GetUserName() + " disconnected.");
            });

            this.m_WaitingToQuit.clear();

            if (!this.m_IsGameOver
                    && this.m_MatchPlayers.size() < GlobalSettings.MINIMUM_AMOUNT_OF_PLAYERS)
            {
                throw new Exception(GlobalSettings.NOT_ENOUGH_PLAYERS_TO_CONTINUE);
            }
        }
    }

    private void waitForPlayersToBeReady() throws Exception {

        this.SendToAll(new ClientMessage(ClientMessage.MessageType.CONFIRMATION, GlobalSettings.PLAYER_READY_MESSAGE).toString());
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

    private String getMatchPlayersAsJson() {

        Map<String, JsonObject> playersMap = new HashMap<>();
        this.actionOnMatchPlayers((player) -> playersMap.put(player.GetUserName(), player.GetPlayerMatchData()));

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
            MatchLogger.Error(GetMatchIdentifier(), i_MatchEndedReason);
        else
            MatchLogger.Info(GetMatchIdentifier(), i_MatchEndedReason);

        // TODO - update players coins and stats on database.
        //  /30.4/UPDATE - only stats left.
        this.actionOnMatchPlayers(p -> DBHandler.updateStatsInDB(p.GetCharacter()));
        this.actionOnMatchPlayers(p -> p.CloseConnection(GlobalSettings.MATCH_ENDED));

        MatchMaking.RemoveActiveMatch(this);
        this.interrupt();
    }
}


/*
// should read the input from socket's buffer for each player.
            for (Player player : m_MatchPlayers) {
                try {
                    BufferedReader in = player.GetInStream();
                    PrintWriter out = player.GetOutStream();

                    // collect all the input that the client send in the past 200ms.
                    if (in.ready()) {
                        String inputLine = in.readLine(); // Read last line.
                        LoggerManager.info(player.GetCharacterName() + ": " + inputLine);

                        //TODO: store client's data somehow, and update his process.
                        out.println("Data received.");
                    }

                } catch (IOException e) {
                    LoggerManager.debug("Something went wrong with player " + player.GetCharacterName());
                    throw new RuntimeException(e);
                }
            }
 */