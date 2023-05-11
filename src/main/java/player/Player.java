package player;
import addons.Character;
import addons.Location;
import com.google.gson.JsonObject;
import dto.ClientMessage;
import interfaces.Match;
import json.JsonFormatter;
import okhttp3.*;
import utils.LoggerManager;

import match_making.MatchMaking;
import utils.Utils;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class Player implements Comparable {

    private String m_UserName;
    private Match m_CurrentMatch;
    private Location m_Location;
    private Character m_Character;
    private final Socket m_SocketConnection; // Client's socket.
    private PrintWriter m_OutStream; // Output stream.
    private BufferedReader m_InStream; // Input stream.
    private boolean m_IsReady;
    private boolean m_IsOnline;

    // GameSession constructor.
    public Player(Socket socketConnection) throws SocketException {

        this.m_SocketConnection = socketConnection;

        try {

            this.m_OutStream = new PrintWriter(this.m_SocketConnection.getOutputStream(), true);
            this.m_InStream = new BufferedReader(new InputStreamReader(this.m_SocketConnection.getInputStream()));

            String initData = m_InStream.readLine(); // Get initialization data from client in json (contains userid & characterId).
            fetchPlayerData(JsonFormatter.createJsonFromString(initData));

            this.m_IsOnline = true;
            this.m_IsReady = false;

            this.sendMessage(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Connection established.").toString());

        } catch (Exception e) {
            LoggerManager.error("Error occurred with " + this.getHost() + ": " + e.getMessage());
            HandleClientError(e);
            closeConnection();
        }
    }

    private void HandleClientError(Exception e) {
        sendMessage(new ClientMessage(ClientMessage.MessageType.ERROR, e.getMessage()).toString());
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

        JsonObject resData = JsonFormatter.createJsonFromString(response.body().string());
        this.m_UserName = String.valueOf((resData).get("username"));
        m_Character = JsonFormatter.GetGson().fromJson(resData, Character.class);

        // TODO - delete
        this.m_OutStream.println(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Server fetched data."));

    }

    // Send message to customer.
    public void sendMessage(String message) {
        try {
            m_OutStream.println(message);
        } catch (Exception e) {

            if (e instanceof SocketException) {
                // TODO - Handle player terminated client.
            }

            LoggerManager.error(e.getMessage());
        }
    }

    public void setNewMatch(Match n_Match) {
        this.m_CurrentMatch = n_Match;
        this.m_Location = new Location(0, 0); //probably not necessary.
    }

    // Close socket connection when player exists while match started.
    public void closeConnection() {

        if (this.m_SocketConnection.isClosed())
            return;

        try {
            LoggerManager.info("Socket (" + this.getHost() + "): Connection closed!");
            MatchMaking.removePlayerFromWaitingList(this);

            // If player was already in a game.
            if (this.m_CurrentMatch != null && !this.m_CurrentMatch.isM_IsGameOver())
                this.m_CurrentMatch.removePlayerFromMatch(this);

            this.m_SocketConnection.close();
        } catch (Exception e) {
            LoggerManager.error(e.getMessage());
        }
    }

    // Get host address as known as IP address.
    public String getHost() {
        return this.m_SocketConnection.getInetAddress().getHostAddress();
    }

    public boolean isConnectionAlive() {

        try {
            ClientMessage heartbeat = new ClientMessage(ClientMessage.MessageType.CONFIRMATION, "isAlive\n");
            String heartbeatReadyToSend = heartbeat.toString() + "\n";
            this.m_SocketConnection.getOutputStream().write(heartbeatReadyToSend.getBytes());
        } catch (IOException e) {
            if (e instanceof SocketException) {
                this.closeConnection();
                return false;
            }
        }

        return true;

    }

    public PrintWriter getM_OutStream() {
        return m_OutStream;
    }

    public BufferedReader getM_InStream() {
        return m_InStream;
    }

    public Character getM_Character() {
        return this.m_Character;
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
        return this.m_Character.getCharacterName();
    }

    public String getM_UserName() {
        return this.m_UserName;
    }

    public JsonObject getAppearanceDataAsJson(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("characterId", m_Character.getCharacterID());
        jsonObject.addProperty("characterName", m_Character.getCharacterName());
        jsonObject.addProperty("playerUsername", this.getM_UserName());
        return jsonObject;
    }

    public String readMessage(){
        try {
            String msg = getM_InStream().readLine();
            return msg;
        } catch (IOException e) {

            //TODO - handle player client termination.
            return null;
        }
    }

    public boolean isM_IsReady(){
        return this.m_IsReady;
    }

    public void setReady(){
        this.m_IsReady = true;
    }

}
