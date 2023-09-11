package entities.player;

import com.google.gson.JsonObject;
import dto.ServerGeneralMessage;
import model.player.PlayerEntity;
import model.connection.ConnectionModel;
import utils.player.Character;
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

public class PCPlayerEntity extends PlayerEntity implements Comparable<PCPlayerEntity>
{

    public PCPlayerEntity(ConnectionModel i_SocketConnection) throws IOException
    {
        super(i_SocketConnection);

        String initData = this.m_Connection.WaitForPlayerResponse();
        LoggerManager.debug("Received data from socket " + this.m_Connection.getHost());

        fetchPlayerData(JsonFormatter.createJsonFromString(initData));
        LoggerManager.debug("Player " + this.GetUserName() + " log: fetched character data successfully.");

        LoggerManager.debug("Player " + this.m_Username + " has been created!");
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
        this.m_Username = String.valueOf((resData).get("username")).replace("\"", "").toLowerCase();
        m_Character = JsonFormatter.GetGson().fromJson(resData, Character.class);
    }

    @Override
    public void CloseConnection(String i_ExceptionMessage)
    {
        if(!this.m_Connection.IsConnected())
            return;

        if(this.m_CurrentMatch != null)
            this.m_CurrentMatch.RemovePlayerFromMatch(this);
        else
            MatchMaking.RemovePlayerFromQueue(this);

        // Notify player on connection close.
        try {
            this.SendMessage(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, i_ExceptionMessage).toString());
        } catch(SocketTimeoutException ste) {
            LoggerManager.warning("Couldn't notify player " + this.m_Username + " on " + i_ExceptionMessage);
        }

        MarkDead();
        this.m_Connection.CloseConnection(i_ExceptionMessage);
    }

    public boolean EqualByUsername(String i_Username)
    {
        return this.m_Username.equals(i_Username);
    }

    public void MarkAsFinish()
    {
        this.m_IsFinished = true;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PCPlayerEntity matchPCPlayerEntity = (PCPlayerEntity) o;
        return m_Username.equals(matchPCPlayerEntity.m_Username);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_Username);
    }

    @Override
    public int compareTo(@NotNull PCPlayerEntity o) {
        return 0;
    }
}
