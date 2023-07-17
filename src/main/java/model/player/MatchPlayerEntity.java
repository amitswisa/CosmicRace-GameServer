package model.player;

import com.google.gson.JsonObject;
import model.connection.Connection;
import services.MatchService;
import services.OnlineMatchService;
import utils.player.Character;
import utils.player.Location;

import java.io.IOException;
import java.net.SocketTimeoutException;

public abstract class MatchPlayerEntity
{
    protected final Connection m_Connection;
    protected MatchService m_CurrentMatch;
    protected boolean m_IsReady;
    protected String m_Username;
    protected int m_CoinsCollected;
    protected boolean m_IsFinished;
    protected Location m_Location;
    protected Character m_Character;

    public MatchPlayerEntity(Connection i_Connection)
    {
        this.m_Connection = i_Connection;
        this.m_IsReady = false;
        this.m_IsFinished = false;
        this.m_CoinsCollected = 0;
    }

    public final void SendMessage(String i_Message) throws SocketTimeoutException
    {
        this.m_Connection.SendMessage(i_Message);
    }

    public final String ReadMessage() throws IOException
    {
        return this.m_Connection.ReadMessage();
    }

    public boolean IsConnectionAlive()
    {
        return this.m_Connection.IsConnectionAlive();
    }

    public final boolean IsReady()
    {
        return this.m_IsReady;
    }

    public final void MarkAsReady()
    {
        this.m_IsReady = true;
    }

    public final String GetUserName()
    {
        return m_Username;
    }

    public final void UpdateLocation(Location i_PlayerLastLocation)
    {
        this.m_Location = i_PlayerLastLocation;
    }

    public void MarkAsFinish()
    {
        this.m_IsFinished = true;
    }

    public boolean EqualByUsername(String i_Username)
    {
        return this.m_Username.equals(i_Username);
    }

    public final Character GetCharacter()
    {
        return this.m_Character;
    }

    public final JsonObject GetPlayerMatchData()
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("playerUsername", this.GetUserName());
        jsonObject.addProperty("CharacterData", this.GetCharacter().GetAsJson());
        return jsonObject;
    }

    public final void CoinCollected()
    {
        this.m_CoinsCollected += 1;
    }

    public boolean IsFinishedMatch()
    {
        return this.m_IsFinished;
    }

    abstract public void CloseConnection(String i_ExceptionMessage);

    public void SetMatch(MatchService i_MatchRef)
    {
        this.m_CurrentMatch = i_MatchRef;
        this.m_Location = new Location(0,0);
    }
}
