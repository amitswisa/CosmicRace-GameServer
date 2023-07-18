package model.connection;

import utils.GlobalSettings;

import java.io.IOException;
import java.net.SocketTimeoutException;

public abstract class ConnectionModel
{
    protected boolean m_IsConnected;
    private long m_LastConnectionTime;

    public ConnectionModel()
    {
        this.m_IsConnected = true;
        this.UpdateLastConnectionTime();
    }

    protected void UpdateLastConnectionTime()
    {
        this.m_LastConnectionTime = System.currentTimeMillis();
    }

    protected boolean isTimedOut()
    {
        return (System.currentTimeMillis() - this.m_LastConnectionTime > GlobalSettings.MAX_TIME_OUT);
    }

    protected boolean ValidateConnectionNeeded()
    {
        return (System.currentTimeMillis() - this.m_LastConnectionTime > 5000);
    }

    abstract public void SendMessage(String i_Message) throws SocketTimeoutException;
    abstract public void CloseConnection(String i_ExceptionMessage);
    abstract public boolean IsConnectionAlive();
    abstract public String ReadMessage() throws IOException;
    abstract public String getHost();
    abstract public String WaitForPlayerResponse() throws IOException;
    abstract public void AddMessageToQueue(String i_Message);
}