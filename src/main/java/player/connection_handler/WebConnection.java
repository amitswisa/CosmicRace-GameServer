package player.connection_handler;

import dto.ServerGeneralMessage;
import interfaces.PlayerConnection;
import jakarta.websocket.Session;
import utils.GlobalSettings;
import utils.loggers.LoggerManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketTimeoutException;

public final class WebConnection implements PlayerConnection {

    ///there's another Session in other library - ic case something isn't working.

    private final Session m_SessionConnection;
    private boolean m_IsConnected;
    private long m_LastClientConnectionTime;

    public WebConnection(Session i_PlayerSession) {

        this.m_SessionConnection = i_PlayerSession;
        this.m_IsConnected = true;

        try {
            this.updateLastClientConnectionTime();
            this.SendMessage(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, "Connecting to server...").toString());

        } catch (Exception e) {
            //not quite sure if it's good.
            CloseConnection(e.getMessage());
        }
    }

    @Override
    public void SendMessage(String i_Message) throws SocketTimeoutException {
        if (isTimedOut())
            throw new SocketTimeoutException(GlobalSettings.TERMINATE_DUE_TO_TIME_OUT);

        try {
            String msg = i_Message + "\n";
            this.m_SessionConnection.getBasicRemote().sendText(msg);

            this.updateLastClientConnectionTime();
        } catch (IOException ioe) {
            throw new SocketTimeoutException(GlobalSettings.NO_CONNECTION);
        }
    }

    @Override
    public void CloseConnection(String i_ExceptionMessage) {

        if (!this.m_IsConnected)
            return;

        try {
            this.m_IsConnected = false;
            this.m_SessionConnection.close();

            if (!i_ExceptionMessage.equals(GlobalSettings.MATCH_ENDED))
                LoggerManager.error("WebSocket (" + this.getHost() + "): " + i_ExceptionMessage);

        } catch (IOException e) {
            LoggerManager.error("Socket (" + this.getHost() + "): " + e.getMessage());
        }
    }

    @Override
    public boolean IsConnectionAlive() {
        if (!this.m_IsConnected)
            return false;

        if(System.currentTimeMillis() - this.m_LastClientConnectionTime <= 5000){
            return true;
        }

        try {
            ServerGeneralMessage heartbeat = new ServerGeneralMessage(ServerGeneralMessage.eActionType.CONFIRMATION, GlobalSettings.SERVER_HEARTBEAT_MESSAGE);
            String heartbeatJson = heartbeat.toString() + "\n";

            this.m_SessionConnection.getBasicRemote().sendText(heartbeatJson);

            this.updateLastClientConnectionTime();
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
    public String ReadMessage () throws IOException { // not sure if we need it, because of the annotations.
        return null;
    }


    /**
     * @return - Message received by client
     * @throws IOException - When null is read from client's input stream, indicates of connection termination.
     */
    @Override
    public String WaitForPlayerResponse() throws IOException { // not sure how to implement it.
        while(!isTimedOut())
        {
            try{

                String initData = this.ReadMessage();

                if(initData.equals(GlobalSettings.NO_MESSAGES_IN_CLIENT_BUFFER))
                    continue;

                return initData;

            } catch (Exception e) {
                this.CloseConnection(e.getMessage());
                break;
            }
        }

        throw new IOException(GlobalSettings.TERMINATE_DUE_TO_TIME_OUT);
    }

    @Override
    public String getHost () {
        return this.m_SessionConnection.getRequestURI().getHost();
    }

    @Override
    public BufferedReader GetInStream () { //there's no option to read it, so maybe it is unnecessary;
        return null;
    }

    private boolean isTimedOut ()
    {
        return (System.currentTimeMillis() - this.m_LastClientConnectionTime > GlobalSettings.MAX_TIME_OUT);
    }

    private void updateLastClientConnectionTime () {
        this.m_LastClientConnectionTime = System.currentTimeMillis();
    }
}






