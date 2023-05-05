package client;
import addons.Character;
import addons.Location;
import com.google.gson.Gson;
import interfaces.Match;
import utils.LoggerManager;

import utils.MatchMaking;
import utils.Utils;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Logger;

public class Player implements Comparable/*, AutoCloseable */ {
    private Match currentMatch;
    private Location location;
    private Character character;
    private final Socket socketConnection; // Client's socket.
    private PrintWriter out_stream; // Output stream.
    private BufferedReader in_stream; // Input stream.
    private boolean waitingToPlay;
    private boolean isOnlinePlayer;
    public static final Gson gson = new Gson();

    // GameSession constructor.
    public Player(Socket socketConnection) {

        this.socketConnection = socketConnection;
        try {
            // Get client's I/O tunnels.
            this.out_stream = new PrintWriter(this.socketConnection.getOutputStream(), true);
            this.in_stream = new BufferedReader(new InputStreamReader(this.socketConnection.getInputStream()));
            LoggerManager.info("Player (" + this.getHost() + ") connected to server!");

            // Get initialization data from client and parse it.

            //TODO: NOTE: probably don't need this anymore, because parsing occurs in "getCharacterInfoFromSocket".
            /*String init_Data = in_stream.readLine();
            JsonObject parser = JsonParser.parseString(init_Data).getAsJsonObject();

            this.playerName = parser.get("username").getAsString();
            this.location = new Location(parser.getAsJsonObject("location").get("x").getAsDouble()
                    , parser.getAsJsonObject("location").get("y").getAsDouble());*/

            this.waitingToPlay = true;

            //TODO: need to get socket's header.
            //---------------
            isOnlinePlayer = true;
            //---------------

            getCharacterJsonFromSocket();

        } catch (Exception e) {
            LoggerManager.error("Error occurred with " + this.getHost() + ": " + e.getMessage());
            closeConnection();
        }
    }

    private void getCharacterJsonFromSocket() {
        Gson gson = new Gson();
        try {
            character = gson.fromJson(in_stream.readLine(), Character.class);
        } catch (IOException e) {
            throw new RuntimeException("couldn't read client's data.");
        }

        Utils.printCharacter(character);
    }

    // Send message to customer.
    public void sendMessage(String message) {
        try {
            out_stream.println(message);
        } catch (Exception e) {

            if (e instanceof SocketException) {
                // TODO - Handle player teminated client.
            }

            LoggerManager.error(e.getMessage());
        }
    }

    public void publishPlayerDetails() {
        try {
            String playerDataJson = gson.toJson(character, Character.class);
            out_stream.println(playerDataJson);
        } catch (Exception e) {
            LoggerManager.error("player " + getPlayerName() + "couldn't send his details for some reason.");
        }
    }

    public void setNewMatch(Match n_Match) {
        this.currentMatch = n_Match;
        this.location = new Location(0, 0); //probably not necessary.
    }

    // Close socket connection when player exists while match started.
    public void closeConnection() {

        if (this.socketConnection.isClosed())
            return;

        try {
            LoggerManager.info("Socket (" + this.getHost() + "): Connection closed!");
            MatchMaking.removePlayerFromWaitingList(this);

            // If player was already in a game.
            if (this.currentMatch != null && !this.currentMatch.isGameOver())
                this.currentMatch.removePlayerFromMatch(this);

            this.socketConnection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // Get host address as known as IP address.
    public String getHost() {
        return this.socketConnection.getInetAddress().getHostAddress();
    }

    private Socket getSocketConnection() {
        return socketConnection;
    }

    public boolean isConnectionAlive() {

        try {
            String heartbeat = "isAlive\n";
            this.socketConnection.getOutputStream().write(heartbeat.getBytes());
        } catch (IOException e) {
            if (e instanceof SocketException) {
                this.closeConnection();
                return false;
            }
        }

        return true;

    }

    public PrintWriter getOut_stream() {
        return out_stream;
    }

    public BufferedReader getIn_stream() {
        return in_stream;
    }

    public Character getCharacter() {
        return this.character;
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj);
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    public String getPlayerName() {
        return this.character.getCharacterName();
    }


 /*   @Override
    public void close() throws Exception {
        closeConnection();
    }*/


}
