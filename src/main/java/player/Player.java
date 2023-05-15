package player;
import addons.Character;
import addons.Location;
import com.google.gson.JsonObject;
import interfaces.Match;
import json.JsonFormatter;
import okhttp3.*;

import match_making.MatchMaking;
import utils.GlobalSettings;
import utils.logs.LoggerManager;

import java.io.*;
import java.net.Socket;
import java.util.Objects;

public class Player implements Comparable {

    private final PlayerConnection m_PlayerConnection;
    private Match m_CurrentMatch;
    private Character m_Character;
    private Location m_Location;
    private String m_UserName;
    private boolean m_IsReady;

    public Player(Socket socketConnection) throws IOException
    {
        this.m_PlayerConnection = new PlayerConnection(socketConnection);

        String initData = this.m_PlayerConnection.WaitForPlayerResponse();
        fetchPlayerData(JsonFormatter.createJsonFromString(initData));

        this.m_IsReady = false;
        LoggerManager.info("Player " + this.m_UserName + " has been created!");
    }

    // TODO - maybe static ?
    private void fetchPlayerData(JsonObject i_InitData) throws IOException
    {
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new FormBody.Builder()
                .add("userid", String.valueOf(i_InitData.get("userid")))
                .add("characterId", String.valueOf(i_InitData.get("characterId")))
                .build();

        Request request = new Request.Builder()
                .url(GlobalSettings.WEB_API_URL + "/fetchCharacterData")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException(GlobalSettings.COULDNT_FETCH_ERROR);
        }

        JsonObject resData = JsonFormatter.createJsonFromString(response.body().string());
        this.m_UserName = String.valueOf((resData).get("username"));
        m_Character = JsonFormatter.GetGson().fromJson(resData, Character.class);
    }

    // Send message to customer.
    public final void setNewMatch(Match i_Match)
    {
        this.m_CurrentMatch = i_Match;
        this.m_Location = new Location(0, 0); //probably not necessary.
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
            return this.m_PlayerConnection.ReadMessage();
        } catch(Exception e) {
            this.CloseConnection(e.getMessage());
        }

        return GlobalSettings.NO_CONNECTION;
    }

    public final boolean IsReady()
    {
        return this.m_IsReady;
    }

    public final void MarkAsReady()
    {
        this.m_IsReady = true;
    }

    public void SendMessage(String i_Message)
    {
        this.m_PlayerConnection.sendMessage(i_Message);
    }

    public final void CloseConnection(String i_ExceptionMessage)
    {
        MatchMaking.RemovePlayerFromWaitingList(this);
        this.m_PlayerConnection.CloseConnection(i_ExceptionMessage);
    }

    // Get host address as known as IP address.
    public final String getHost()
    {
        return this.m_PlayerConnection.getHost();
    }

    public final boolean IsConnectionAlive()
    {
        return this.m_PlayerConnection.IsConnectionAlive();
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
        return m_UserName.equals(player.m_UserName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_UserName);
    }

}
