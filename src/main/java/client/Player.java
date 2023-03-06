package client;
import addons.Character;
import addons.Location;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import utils.LoggerManager;
import utils.Match;
import utils.MatchMaking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

public class Player extends Thread implements Comparable {

    private String playerName;
    private Match currentMatch;
    private Location location;
    private Character character;

    private final Socket socketConnection; // Client's socket.
    private PrintWriter out_stream; // Output stream.
    private BufferedReader in_stream; // Input stream.

    private boolean waitingToPlay;

    // GameSession constructor.
    public Player(Socket socketConnection) {

        this.socketConnection = socketConnection;
        try {
            // Get client's I/O tunnels.
            this.out_stream = new PrintWriter(this.socketConnection.getOutputStream(), true);
            this.in_stream = new BufferedReader(new InputStreamReader(this.socketConnection.getInputStream()));
            LoggerManager.info("Player (" +this.getHost()+ ") connected to server!");

            // Get initialization data from client and parse it.
            String init_Data = in_stream.readLine();
            JsonObject parser = JsonParser.parseString(init_Data).getAsJsonObject();

            this.playerName = parser.get("username").getAsString();
            this.location = new Location(parser.getAsJsonObject("location").get("x").getAsDouble()
                    , parser.getAsJsonObject("location").get("y").getAsDouble());
            // TODO - Get Character init settings from json.

            this.waitingToPlay = true;

            MatchMaking.addPlayerToWaitingList(this); // Add socket to socket's list.

        } catch(Exception e) {
            LoggerManager.error("Error occurred with " + this.getHost() + ": " + e.getMessage());
            closeConnection();
        }
    }

    @Override
    public void run() {

        try {
            String line;
            while ((line = this.in_stream.readLine()) != null) {
                // Handle client request to play online.

            }

        } catch (IOException e) {
            // TODO - remove player from match.
            LoggerManager.debug("Error handling client ("+ this.getHost() +"): " + e.getMessage());
            closeConnection();
        }

    }

    // Send message to customer.
    public void sendMessage(String message) {
        try {
            out_stream.println(message);
        } catch(Exception e) {
            LoggerManager.error(e.getMessage());
        }
    }

    public void setNewMatch(Match n_Match) {
        this.currentMatch = n_Match;
        this.location = new Location(0,0);
    }

    // Close socket connection.
    public void closeConnection() {

        if(this.socketConnection.isClosed())
            return;

        try {

            LoggerManager.info("Socket (" + this.getHost() + "): Connection closed!");
            MatchMaking.removePlayerFromWaitingList(this); // Remove from waiting to play list.

            // If player was already in a game.
            if(this.currentMatch != null && !this.currentMatch.isGameOver())
                this.currentMatch.removePlayerFromMatch(this);

            this.socketConnection.close();
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public String getPlayerName() {
        return this.playerName;
    }

    // Get host address as known as IP address.
    public String getHost() {
        return this.socketConnection.getInetAddress().getHostAddress();
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj);
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
