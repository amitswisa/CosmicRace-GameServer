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
        super();
        this.r_Connection = i_Session;
        m_MessagesQueue = new PriorityQueue<>();
    }

    @Override
    public void SendMessage(String i_Message) throws SocketTimeoutException
    {
        try {
            r_Connection.getBasicRemote().sendText(i_Message);
            this.UpdateLastConnectionTime();
        } catch (IOException e) {
            throw new SocketTimeoutException(GlobalSettings.NO_CONNECTION);
        }
    }

    @Override
    public boolean IsConnectionAlive()
    {
        if (!this.m_IsConnected)
            return false;

        if(ValidateConnectionNeeded())
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
        if(isTimedOut() || !IsConnectionAlive())
            throw new SocketTimeoutException(GlobalSettings.TERMINATE_DUE_TO_TIME_OUT);

        if(m_MessagesQueue.isEmpty())
            return GlobalSettings.NO_MESSAGES_IN_CLIENT_BUFFER;

        String message = m_MessagesQueue.poll();

        UpdateLastConnectionTime();

        if(message.equals(GlobalSettings.CLIENT_HEARTBEAT_RESPONSE))
        {
            return GlobalSettings.NO_MESSAGES_IN_CLIENT_BUFFER;
        }
        else
        {
            return message;
        }
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

    @Override
    public void CloseConnection(String i_ExceptionMessage) {

        this.m_IsConnected = false; // Set to false immediately

        if (r_Connection == null || !r_Connection.isOpen()) {
            return; // Check if the connection is already closed or null
        }

        try {
            r_Connection.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, i_ExceptionMessage));

            if (!i_ExceptionMessage.equals(GlobalSettings.MATCH_ENDED))
                LoggerManager.error("WebSocket (" + this.getHost() + "): " + i_ExceptionMessage);
        } catch (IOException e) {
            LoggerManager.error("WebSocket IO Exception (" + this.getHost() + "): " + e.getMessage());
        } catch (Exception e) {
            LoggerManager.error("WebSocket General Exception (" + this.getHost() + "): " + e.getMessage());
        }
    }
}






