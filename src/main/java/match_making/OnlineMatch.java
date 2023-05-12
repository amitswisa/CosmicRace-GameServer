package match_making;

import addons.Character;
import json.JsonFormatter;
import player.Player;
import com.google.gson.JsonObject;
import dto.ClientMessage;
import interfaces.Match;
import utils.singletons.DBHandler;
import utils.LoggerManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class OnlineMatch extends Thread implements Match {

    private List<Player> m_MatchPlayers;
    private final String m_MatchIdentifier;
    private boolean m_IsGameOver;

    public OnlineMatch(String i_MatchIdentifier, List<Player> i_MatchPlayers) {

        // Log players currently playing.
        LoggerManager.info("New match! Players: ");
        i_MatchPlayers.forEach(player -> LoggerManager.info(player.getPlayerName()));

        // Update current match for all players.
        this.m_IsGameOver = false;
        this.m_MatchIdentifier = i_MatchIdentifier;
        this.m_MatchPlayers = i_MatchPlayers;

        // Init settings.
        this.m_MatchPlayers.forEach(player -> player.setNewMatch(this));

        // Message deliver.
        SendToAll(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Players found, creating a match.").toString());

        // Broadcast players character data.
        SendToAll(new ClientMessage(ClientMessage.MessageType.DATA, createPlayersAppearanceJson()).toString());
    }

    @Override
    public void run() {

        this.SendToAll(new ClientMessage(ClientMessage.MessageType.CONFIRMATION, "READY?").toString());

        this.waitForPlayersConfirmation();

        this.SendToAll(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Starting match..").toString());

        this.SendToAll(new ClientMessage(ClientMessage.MessageType.ACTION, "START").toString());

        while (!m_IsGameOver) {
            // should read the input from socket's buffer for each player.
            for (Player player : m_MatchPlayers) {
                try {
                    BufferedReader in = player.GetInStream();
                    PrintWriter out = player.GetOutStream();

                    // collect all the input that the client send in the past 200ms.
                    if (in.ready()) {
                        String inputLine = in.readLine(); // Read last line.
                        LoggerManager.info(player.getPlayerName() + ": " + inputLine);

                        //TODO: store client's data somehow, and update his process.
                        out.println("Data received.");
                    }

                } catch (IOException e) {
                    LoggerManager.debug("Something went wrong with player " + player.getPlayerName());
                    throw new RuntimeException(e);
                }
            }

            try {
                // collecting data from client each 200ms.
                this.SendToAll("Players Update!"); //sending here players Jsons Details.
                LoggerManager.info("Going to sleep 200ms.");
                LoggerManager.info("Amount of players in current match is: " + m_MatchPlayers.size());
                Thread.sleep(200);
            } catch (InterruptedException e) {
                LoggerManager.error(e.getMessage());
                throw new RuntimeException(e);
            }

        }

        EndMatch();
    }

    @Override
    public boolean IsGameOver() {
        return this.m_IsGameOver;
    }

    @Override
    public void RemovePlayerFromMatch(Player player) {
        this.m_MatchPlayers.remove(player);

        // if there's only one player left in the game.
        if (m_MatchPlayers.size() == 1)
            this.m_IsGameOver = true;

        LoggerManager.info("Player " + player.getPlayerName() + " has left the game!");
        this.SendToAll("Player " + player.getPlayerName() + " has left the game!"); // Send player left message to remaining players.
    }

    @Override
    public void EndMatch() {
        // Set all player's current match to null.
        LoggerManager.info("Match ended!");

        // TODO - update players coins and stats on database.
        //  /30.4/UPDATE - only stats left.
        this.m_MatchPlayers.forEach(player -> {
            DBHandler.updateStatsInDB(player.GetCharacter());
        });

        MatchMaking.RemoveActiveMatch(this);
        this.m_MatchPlayers.forEach(player -> player.closeConnection());
    }

    public void SendToAll(String message){
        m_MatchPlayers.forEach(player -> player.sendMessage(message));
        LoggerManager.info("Message to all players: " + message);
    }

    public String GetMatchIdentifier() {
        return this.m_MatchIdentifier;
    }

    private void updatePlayersStats(List<Character> characterList) {

    }

    private void waitForPlayersConfirmation() {
        AtomicBoolean isEveryoneReady = new AtomicBoolean(false);

        do {
            isEveryoneReady.set(true);
            m_MatchPlayers.forEach(player -> {
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

    private String createPlayersAppearanceJson() {
        // Create a map to hold the player objects
        Map<String, JsonObject> playersMap = new HashMap<>();
        this.m_MatchPlayers.forEach((player) -> playersMap.put(player.GetUserName(), player.GetCharacterData()));

        // Create the main JSON object and add the "MatchIdentifyer" and "Players" properties
        JsonObject mainObject = new JsonObject();
        mainObject.addProperty("MatchIdentifier", this.GetMatchIdentifier());
        mainObject.add("Players", JsonFormatter.GetGson().toJsonTree(playersMap));

        // Convert the JSON object to a string and print it
        return JsonFormatter.GetGson().toJson(mainObject);
    }
}
