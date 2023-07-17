package match.entities.match_player;
import com.google.gson.JsonObject;
import dto.ServerGeneralMessage;
import match.entities.MatchEntity;
import servers.connection.Connection;
import match.services.OnlineMatchService;
import match.entities.utils.Character;
import match.entities.utils.Location;
import utils.json.JsonFormatter;
import okhttp3.*;
import match.MatchMaking;
import org.jetbrains.annotations.NotNull;
import utils.GlobalSettings;
import utils.loggers.LoggerManager;
import utils.route.APIRoutes;
import java.io.*;
import java.net.SocketTimeoutException;
import java.util.Objects;

public class MatchPlayerEntity extends MatchEntity implements Comparable<MatchPlayerEntity>
{

    private OnlineMatchService m_CurrentMatch;
    private Character m_Character;
    private Location m_Location;
    private String m_UserName;
    private boolean m_IsReady;
    private boolean m_IsFinished;
    private int m_CoinsCollected;

    public MatchPlayerEntity(Connection i_SocketConnection) throws IOException
    {
        super(i_SocketConnection);
        this.m_CoinsCollected = 0;

        String initData = this.m_Connection.WaitForPlayerResponse();
        LoggerManager.debug("Received data from socket " + this.m_Connection.getHost());

        fetchPlayerData(JsonFormatter.createJsonFromString(initData));
        LoggerManager.debug("Player " + this.GetUserName() + " log: fetched character data successfully.");

        this.m_IsReady = false;
        this.m_IsFinished = false;
        LoggerManager.debug("Player " + this.m_UserName + " has been created!");
    }

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
        this.m_UserName = String.valueOf((resData).get("username")).replace("\"", "");
        m_Character = JsonFormatter.GetGson().fromJson(resData, Character.class);
    }

    public final void SetMatch(OnlineMatchService i_Match)
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
        else
            MatchMaking.RemovePlayerFromQueue(this);

        // Notify player on connection close.
        try {
            this.SendMessage(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, i_ExceptionMessage).toString());
        } catch(SocketTimeoutException ste) {
            LoggerManager.warning("Couldn't notify player " + this.m_UserName + " on " + i_ExceptionMessage);
        }

        this.m_Connection.CloseConnection(i_ExceptionMessage);
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

    public void MarkAsFinish()
    {
        this.m_IsFinished = true;
    }

    public boolean IsFinishedMatch()
    {
        return this.m_IsFinished;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchPlayerEntity matchPlayerEntity = (MatchPlayerEntity) o;
        return m_UserName.equals(matchPlayerEntity.m_UserName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_UserName);
    }

    @Override
    public int compareTo(@NotNull MatchPlayerEntity o) {
        return 0;
    }
}
