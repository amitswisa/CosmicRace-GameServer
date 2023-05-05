package client;
import addons.Character;
import addons.Location;
import com.google.gson.JsonObject;
import interfaces.Match;
import okhttp3.*;
import utils.LoggerManager;

import utils.MatchMaking;
import utils.Utils;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class Player implements Comparable {

    private String username;
    private Match currentMatch;
    private Location location;
    private Character character;
    private final Socket socketConnection; // Client's socket.
    private PrintWriter out_stream; // Output stream.
    private BufferedReader in_stream; // Input stream.
    private boolean isReady;
    private boolean isOnlinePlayer;

    // GameSession constructor.
    public Player(Socket socketConnection) {

        this.socketConnection = socketConnection;
        try {
            // Get client's I/O tunnels.
            this.out_stream = new PrintWriter(this.socketConnection.getOutputStream(), true);
            this.in_stream = new BufferedReader(new InputStreamReader(this.socketConnection.getInputStream()));
            LoggerManager.info("Player (" + this.getHost() + ") connected to server!");

            // Get initialization data from client in json (contains userid & characterId).
            String init_Data = in_stream.readLine();

            fetchPlayerData(Utils.createJsonFromString(init_Data));

            this.isOnlinePlayer = true;
            this.isReady = false;
            this.sendMessage("N: Connection established.");

            // Add socket to socket's list.
            MatchMaking.addPlayerToWaitingList(this);
        } catch (Exception e) {
            LoggerManager.error("Error occurred with " + this.getHost() + ": " + e.getMessage());
            HandleClientError(e);
            closeConnection();
        }
    }

    private void HandleClientError(Exception e) {
        sendMessage("E:" + e.getMessage());
    }

    private void fetchPlayerData(JsonObject init_Data) throws IOException {
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new FormBody.Builder()
                .add("userid", String.valueOf(init_Data.get("userid")))
                .add("characterId", String.valueOf(init_Data.get("characterId")))
                .build();

        Request request = new Request.Builder()
                .url(Utils.WEB_API_URL + "/fetchCharacterData")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Error occurred while fetching character data, please try again later...");
        }

        this.username = String.valueOf(Utils.createJsonFromString(response.body().string()).get("username"));
        character = Utils.gson.fromJson(response.body().string(), Character.class);

    }

    // Send message to customer.
    public void sendMessage(String message) {
        try {
            out_stream.println(message);
        } catch (Exception e) {

            if (e instanceof SocketException) {
                // TODO - Handle player terminated client.
            }

            LoggerManager.error(e.getMessage());
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
            LoggerManager.error(e.getMessage());
        }
    }

    // Get host address as known as IP address.
    public String getHost() {
        return this.socketConnection.getInetAddress().getHostAddress();
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

    public String getUsername() {
        return this.username;
    }

    public JsonObject getAppearanceDataAsJson(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("characterId", character.getCharacterID());
        jsonObject.addProperty("characterName", character.getCharacterName());
        jsonObject.addProperty("playerUsername", this.getUsername());
        return jsonObject;
    }

    public String readMessage(){
        try {
            return getIn_stream().readLine();
        } catch (IOException e) {

            //TODO - handle player client termination.
            return null;
        }
    }

    public boolean isReady(){
        return this.isReady;
    }

    public void setReady(){
        this.isReady = true;
    }

}
