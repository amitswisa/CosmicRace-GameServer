package entities.connection;


import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import model.connection.Connection;
import utils.GlobalSettings;
import utils.loggers.LoggerManager;

import java.io.IOException;
import java.net.SocketTimeoutException;

public final class WebConnection extends Connection {

    private final Session r_Connection;
    public WebConnection(Session i_Session) {
        this.r_Connection = i_Session;

    }

    @Override
    public void SendMessage(String i_Message) throws SocketTimeoutException {
        try {
            r_Connection.getBasicRemote().sendText(i_Message);
        } catch (IOException e) {

            //maybe should throw a SocketTimeoutException.
            CloseConnection(e.getMessage());
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

    @Override //Moses said.
    public boolean IsConnectionAlive() {
        return this.m_IsConnected;
    }

    @Override
    public String ReadMessage() throws IOException {
        return null;
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public String WaitForPlayerResponse() throws IOException {
        return null;
    }
}






