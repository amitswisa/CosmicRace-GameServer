package player;
import addons.Character;
import addons.Location;
import com.google.gson.JsonObject;
import interfaces.Match;
import json.JsonFormatter;
import okhttp3.*;
import match_making.MatchMaking;
import org.jetbrains.annotations.NotNull;
import player.connection_handler.PlayerConnection;
import utils.GlobalSettings;
import utils.logs.LoggerManager;
import utils.messages_manager.APIRoutes;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Objects;

public class Player implements Comparable<Player> {

    private final PlayerConnection m_PlayerConnection;
    private Match m_CurrentMatch;
    private Character m_Character;
    private Location m_Location;
    private String m_UserName;
    private boolean m_IsReady;
    private int m_CoinsCollected;

    public Player(Socket i_SocketConnection) throws IOException
    {
        this.m_PlayerConnection = new PlayerConnection(i_SocketConnection);
        this.m_CoinsCollected = 0;

        String initData = this.m_PlayerConnection.WaitForPlayerResponse();
        LoggerManager.debug("Received data from socket " + this.m_PlayerConnection.getHost());

        fetchPlayerData(JsonFormatter.createJsonFromString(initData));
        LoggerManager.debug("Player " + this.GetUserName() + " log: fetched character data successfully.");

        this.m_IsReady = false;
        LoggerManager.debug("Player " + this.m_UserName + " has been created!");
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
                .url(APIRoutes.GetCompleteRoute(APIRoutes.FETCH_CHARACTER_ROUTE))
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException(GlobalSettings.COULDNT_FETCH_CHARACTER_DATA_ERROR);
        }

        JsonObject resData = JsonFormatter.createJsonFromString(response.body().string());
        this.m_UserName = String.valueOf((resData).get("username"));
        m_Character = JsonFormatter.GetGson().fromJson(resData, Character.class);
    }

    public final void SetMatch(Match i_Match)
    {
        this.m_CurrentMatch = i_Match;
        this.m_Location = new Location(0, 0);
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

    public final String ReadMessage() throws IOException {
        return this.m_PlayerConnection.ReadMessage();
    }

    public final void SendMessage(String i_Message) throws SocketTimeoutException {
        this.m_PlayerConnection.SendMessage(i_Message);
    }

    public final boolean IsReady()
    {
        return this.m_IsReady;
    }

    public final void MarkAsReady()
    {
        this.m_IsReady = true;
    }

    public final Character GetCharacter()
    {
        return this.m_Character;
    }

    public final void CloseConnection(String i_ExceptionMessage)
    {
        if(this.m_CurrentMatch != null)
            this.m_CurrentMatch.RemovePlayerFromMatch(this);

        MatchMaking.RemovePlayerFromQueue(this);

        // Notify player on connection close.
        try {
            this.SendMessage(i_ExceptionMessage);
        } catch(SocketTimeoutException ste) {
            LoggerManager.warning("Couldn't notify player " + this.m_UserName + " on " + i_ExceptionMessage);
        }

        this.m_PlayerConnection.CloseConnection(i_ExceptionMessage);
    }

    public final String getHost()
    {
        return this.m_PlayerConnection.getHost();
    }

    public final boolean IsConnectionAlive()
    {
        boolean isConnectionAlive = this.m_PlayerConnection.IsConnectionAlive();

        if(isConnectionAlive)
            return true;
        else {
            this.CloseConnection(GlobalSettings.TERMINATE_DUE_TO_TIME_OUT);
            return false;
        }
    }

    public final void UpdateLocation(Location i_PlayerLastLocation)
    {
        this.m_Location = i_PlayerLastLocation;
    }

    public final void CoinCollected()
    {
        this.m_CoinsCollected += 1;
    }

    public boolean EqualByUsername(String i_Username)
    {
        return this.m_UserName.equals(i_Username);
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

    @Override
    public int compareTo(@NotNull Player o) {
        return 0;
    }
}
