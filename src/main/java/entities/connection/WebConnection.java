package entities.connection;


import dto.ServerGeneralMessage;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import model.connection.ConnectionModel;
import utils.GlobalSettings;
import utils.loggers.LoggerManager;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.PriorityQueue;
import java.util.Queue;

public final class WebConnection extends ConnectionModel {

    private final Session r_Connection;
    private final Queue<String> m_MessagesQueue;

    public WebConnection(Session i_Session)
    {
        this.r_Connection = i_Session;
        m_MessagesQueue = new PriorityQueue<>();
    }

    @Override
    public void SendMessage(String i_Message) throws SocketTimeoutException
    {
        try {
            r_Connection.getBasicRemote().sendText(i_Message);
        } catch (IOException e) {

            //maybe should throw a SocketTimeoutException.
            CloseConnection(e.getMessage());
        }
    }

    @Override
    public boolean IsConnectionAlive()
    {
        if (!this.m_IsConnected)
            return false;

        if(!ValidateConnectionNeeded())
            return true;

        try {
            ServerGeneralMessage heartbeat = new ServerGeneralMessage(ServerGeneralMessage.eActionType.CONFIRMATION, GlobalSettings.SERVER_HEARTBEAT_MESSAGE);
            String heartbeatJson = heartbeat.toString() + "\n";

            this.SendMessage(heartbeatJson);

            this.UpdateLastConnectionTime();
        } catch (Exception e) {

            // Client timed out.
            if(isTimedOut())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public String ReadMessage() throws IOException
    {
        if(m_MessagesQueue.isEmpty())
            return null;

        UpdateLastConnectionTime();
        return m_MessagesQueue.poll();
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public String WaitForPlayerResponse() throws IOException
    {
        return null;
    }

    @Override
    public void AddMessageToQueue(String i_Message) {
        m_MessagesQueue.add(i_Message);
    }

    public void HandleMessageReceived(String i_Message)
    {
        if(i_Message != null)
        {
            m_MessagesQueue.add(i_Message);
        }
    }

    @Override
    public void CloseConnection(String i_ExceptionMessage) {

        if (!this.m_IsConnected)
            return;

        try {
            this.m_IsConnected = false;
            r_Connection.close(new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, i_ExceptionMessage));

            if(!i_ExceptionMessage.equals(GlobalSettings.MATCH_ENDED))
                LoggerManager.error("WebSocket (" + this.getHost() + "): " + i_ExceptionMessage);

        } catch (Exception e) {
            LoggerManager.error("WebSocket (" + this.getHost() + "): " + e.getMessage());
        }
    }
}






