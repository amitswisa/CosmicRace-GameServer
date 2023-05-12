package player;
import addons.Character;
import addons.Location;
import com.google.gson.JsonObject;
import dto.ClientMessage;
import interfaces.Match;
import json.JsonFormatter;
import okhttp3.*;
import utils.logs.LoggerManager;

import match_making.MatchMaking;
import utils.Utils;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;

public class Player implements Comparable {

    private String m_UserName;
    private Match m_CurrentMatch;
    private Location m_Location;
    private Character m_Character;
    private final Socket m_SocketConnection; // Client's socket.
    private PrintWriter m_OutStream; // Output stream.
    private BufferedReader m_InStream; // Input stream.
    private boolean m_IsReady;
    private boolean m_IsConnected;

    public Player(Socket socketConnection) throws SocketException
    {

        this.m_SocketConnection = socketConnection;

        try {

            this.m_OutStream = new PrintWriter(this.m_SocketConnection.getOutputStream(), true);
            this.m_InStream = new BufferedReader(new InputStreamReader(this.m_SocketConnection.getInputStream()));

            String initData = m_InStream.readLine(); // Get initialization data from client in json (contains userid & characterId).
            fetchPlayerData(JsonFormatter.createJsonFromString(initData));

            this.m_IsConnected = true;
            this.m_IsReady = false;

            this.sendMessage(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Connection established.").toString());

        } catch (Exception e) {
            LoggerManager.error("Error occurred with " + this.getHost() + ": " + e.getMessage());
            handleClientError(e);
        }
    }

    private void handleClientError(Exception e)
    {
        sendMessage(new ClientMessage(ClientMessage.MessageType.ERROR, e.getMessage()).toString());
        closeConnection();
    }

    private void fetchPlayerData(JsonObject init_Data) throws IOException
    {
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
        //this.m_OutStream.println(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Server fetched data."));

    }

    // Send message to customer.
    public final void sendMessage(String message)
    {
        m_OutStream.println(message);
    }

    public final void setNewMatch(Match n_Match)
    {
        this.m_CurrentMatch = n_Match;
        this.m_Location = new Location(0, 0); //probably not necessary.
    }

    // Close socket connection when player exists while match started.
    public final void closeConnection()
    {

        if (!this.m_IsConnected || this.m_SocketConnection.isClosed())
            return;

        try {
            this.m_IsConnected = false;
            LoggerManager.info("Socket (" + this.getHost() + "): Connection closed!");

            MatchMaking.RemovePlayerFromWaitingList(this);

            this.m_SocketConnection.close();
        } catch (Exception e) {
            LoggerManager.error(e.getMessage());
        }
    }

    // Get host address as known as IP address.
    public final String getHost()
    {
        return this.m_SocketConnection.getInetAddress().getHostAddress();
    }

    public final boolean IsConnectionAlive()
    {
        try {
            ClientMessage heartbeat = new ClientMessage(ClientMessage.MessageType.CONFIRMATION, "isAlive\n");
            String heartbeatReadyToSend = heartbeat.toString() + "\n";
            this.m_SocketConnection.getOutputStream().write(heartbeatReadyToSend.getBytes());
        } catch (Exception e) {
            return false;
        }

        return true;

    }

    public final PrintWriter GetOutStream()
    {
        return m_OutStream;
    }

    public final BufferedReader GetInStream()
    {
        return m_InStream;
    }

    public final Character GetCharacter()
    {
        return this.m_Character;
    }

    public final String GetCharacterName()
    {
        return this.m_Character.getCharacterName();
    }

    public final String GetUserName()
    {
        return this.m_UserName;
    }

    public final JsonObject GetPlayerMatchData()
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("playerUsername", this.GetUserName());
        jsonObject.addProperty("CharacterData", this.GetCharacter().GetAsJson());
        return jsonObject;
    }

    public final String ReadMessage()
    {
        try {
            String msg = GetInStream().readLine();
            return msg;
        } catch (IOException e) {

            //TODO - handle player client termination.
            return null;
        }
    }

    public final boolean IsReady()
    {
        return this.m_IsReady;
    }

    public final void MarkAsReady()
    {
        this.m_IsReady = true;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return m_UserName.equals(player.m_UserName) && m_SocketConnection.equals(player.m_SocketConnection);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_UserName, m_SocketConnection);
    }

}
