package match_making;

import json.JsonFormatter;
import player.Player;
import com.google.gson.JsonObject;
import dto.ClientMessage;
import interfaces.Match;
import utils.GlobalSettings;
import utils.logs.MatchLogger;
import utils.singletons.DBHandler;
import utils.logs.LoggerManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

final class OnlineMatch extends Thread implements Match {

    private List<Player> m_MatchPlayers;
    private List<Player> m_MatchQuitedPlayers;
    private final String m_MatchIdentifier;
    private boolean m_IsGameOver;

    public OnlineMatch(String i_MatchIdentifier, List<Player> i_MatchPlayers) {

        this.m_MatchIdentifier = i_MatchIdentifier;
        this.m_MatchPlayers = i_MatchPlayers;
        this.m_MatchQuitedPlayers = new ArrayList<>(getNumOfPlayerInMatch());
        this.m_IsGameOver = false;
        this.actionOnMatchPlayers(player -> player.setNewMatch(this));

        MatchLogger.Info(GetMatchIdentifier(), MatchLogger.LogType.NOTIFICATION, "New match created!");

        SendToAll(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Players found, creating a match.").toString());
        SendToAll(new ClientMessage(ClientMessage.MessageType.DATA, getMatchPlayersAsJson()).toString());
    }

    @Override
    public void run() {

        this.SendToAll(new ClientMessage(ClientMessage.MessageType.CONFIRMATION, "READY?").toString());

        this.waitForPlayersConfirmation();

        this.SendToAll(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Starting match..").toString());

        this.SendToAll(new ClientMessage(ClientMessage.MessageType.ACTION, "START").toString());

        while (!m_IsGameOver) {

            try {
                // collecting data from client each 200ms.
                this.SendToAll("Players Update!"); //sending here players Jsons Details.
                LoggerManager.info("Going to sleep 200ms.");
                LoggerManager.info("Amount of players in current match is: " + m_MatchPlayers.size());
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }

        }

        EndMatch();
    }

    @Override
    public void RemovePlayerFromMatch(Player player)
    {
        this.m_MatchPlayers.remove(player);
        MatchLogger.Info(GetMatchIdentifier(), MatchLogger.LogType.NOTIFICATION, "Player " + player.GetUserName() + " has left the game!");
    }

    @Override
    public void EndMatch() {

        this.m_IsGameOver = true;
        MatchLogger.Info(GetMatchIdentifier(), MatchLogger.LogType.NOTIFICATION, "Match ended!");

        // TODO - update players coins and stats on database.
        //  /30.4/UPDATE - only stats left.
        this.actionOnMatchPlayers(p -> DBHandler.updateStatsInDB(p.GetCharacter()));
        this.actionOnMatchPlayers(p -> p.CloseConnection());

        MatchMaking.RemoveActiveMatch(this);
        this.interrupt();
    }

    public void SendToAll(String message){
        actionOnMatchPlayers(player -> player.SendMessage(message));
        MatchLogger.Info(GetMatchIdentifier(), MatchLogger.LogType.ALL_MESSAGE, message);
    }

    public String GetMatchIdentifier() {
        return this.m_MatchIdentifier;
    }

    private void waitForPlayersConfirmation() {
        AtomicBoolean isEveryoneReady = new AtomicBoolean(false);

        do {
            isEveryoneReady.set(true);
            actionOnMatchPlayers(player -> {
                if (!player.IsReady()) {
                    String msg = player.ReadMessage();
                    LoggerManager.info(msg);
                    if (!msg.equals("READY"))
                        isEveryoneReady.set(false);
                    else
                        player.MarkAsReady();
                }
            });
        }
        while (!isEveryoneReady.get());
    }

    private void actionOnMatchPlayers(Consumer<Player> processor) {

        int connectedPlayersNum = 0;
        List<Player> playersToRemove = new ArrayList<>();

        for (Player player : m_MatchPlayers) {
            if (player.IsConnectionAlive()) {
                try {
                    processor.accept(player);
                    connectedPlayersNum++;
                } catch (Exception e) {
                    playersToRemove.add(player);
                }
            }
        }

        this.m_MatchPlayers.removeAll(playersToRemove);
        this.m_MatchQuitedPlayers.addAll(playersToRemove);

        if (!m_IsGameOver && connectedPlayersNum < GlobalSettings.MINIMUM_AMOUNT_OF_PLAYERS)
        {
            EndMatch();
        }
    }

    private String getMatchPlayersAsJson() {

        Map<String, JsonObject> playersMap = new HashMap<>();
        this.actionOnMatchPlayers((player) -> playersMap.put(player.GetUserName(), player.GetPlayerMatchData()));

        JsonObject mainObject = new JsonObject();
        mainObject.addProperty("MatchIdentifier", this.GetMatchIdentifier());
        mainObject.add("Players", JsonFormatter.GetGson().toJsonTree(playersMap));
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