package match.entities;

import servers.connection.Connection;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class MatchEntity
{
    protected final Connection m_Connection;

    public MatchEntity(Connection i_Connection)
    {
        this.m_Connection = i_Connection;
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

}
