package utils;

import addons.Character;

import client.Player;
import com.google.gson.JsonObject;
import interfaces.Match;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultiPlayerMatch extends Thread implements Match{
    private List<Player> players;
    private String identifyer;
    private boolean gameOver;

    public MultiPlayerMatch(String identifyer, List<Player> players) {
        // Update current match for all players.
        this.gameOver = false;
        this.identifyer = identifyer;
        this.players = players;

        // Init settings.
        this.players.forEach(player -> player.setNewMatch(this));

        // Message deliver.
        broadCastToAll("N: Players found, creating a match.");
        broadCastToAll(createPlayersAppearanceJson());
        waitForPlayersConfirmation();
    }

    private void waitForPlayersConfirmation() {
        AtomicBoolean isEveryoneReady = new AtomicBoolean(false);

        do{
            isEveryoneReady.set(true);
            players.forEach(player ->{
                if(!player.isReady()){
                    if(player.readMessage() != "READY")
                        isEveryoneReady.set(false);
                    else
                        player.setReady();
                }
            });
        }
        while(!isEveryoneReady.get());
    }

    private String createPlayersAppearanceJson() {
        // Create a map to hold the player objects
        Map<String, JsonObject> playersMap = new HashMap<>();
        this.players.forEach((player) -> playersMap.put(player.getUsername(), player.getAppearanceDataAsJson()));

        // Create the main JSON object and add the "MatchIdentifyer" and "Players" properties
        JsonObject mainObject = new JsonObject();
        mainObject.addProperty("MatchIdentifier", this.getIdentifyer());
        mainObject.add("Players", Utils.gson.toJsonTree(playersMap));

        // Convert the JSON object to a string and print it
        return Utils.gson.toJson(mainObject);
    }

    @Override
    public void run() {

        // Log match started
        this.broadCastToAll("N: Match Started!");

        // Keep game alive until its over.
        while(!gameOver)
        {
            // should read the input from socket's buffer for each player.
            for (Player player : players) {
                try {
                    BufferedReader in = player.getIn_stream();
                    PrintWriter out = player.getOut_stream();

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
                this.broadCastToAll("Players Update!"); //sending here players Jsons Details.
                LoggerManager.info("Going to sleep 200ms.");
                LoggerManager.info("Amount of players in current match is: " + players.size());
                Thread.sleep(200);
            } catch (InterruptedException e) {
                LoggerManager.error(e.getMessage());
                throw new RuntimeException(e);
            }

        }

        endMatch();
    }

    @Override
    public boolean isGameOver() {
        return this.gameOver;
    }

    @Override
    public void removePlayerFromMatch(Player player) {
        this.players.remove(player);

        // if there's only one player left in the game.
        if(players.size() == 1)
            this.gameOver = true;

        LoggerManager.info("Player " + player.getPlayerName() + " has left the game!");
        this.broadCastToAll("Player " + player.getPlayerName() + " has left the game!"); // Send player left message to remaining players.
    }

    @Override
    public void endMatch() {
        // Set all player's current match to null.
        LoggerManager.info("Match ended!");

        // TODO - update players coins and stats on database.
        //  /30.4/UPDATE - only stats left.
        this.players.forEach(player -> {
            DBHandler.updateStatsInDB(player.getCharacter());
        });

        MatchMaking.removeActiveMatch(this);
        this.players.forEach(player -> player.closeConnection());
    }

    private void updatePlayersStats(List<Character> characterList){

    }


    //Should broadCast JSON.
    public void broadCastToAll(String message){
        players.forEach(player -> player.sendMessage(message));
        LoggerManager.info("Message to all players: " + message);
    }

    public String getIdentifyer() {
        return this.identifyer;
    }
}
