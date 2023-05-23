package player.connection_handler;

import dto.PlayerGeneralMessage;
import utils.GlobalSettings;
import utils.loggers.LoggerManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

public final class PlayerConnection {

    private final Socket m_SocketConnection;
    private PrintWriter m_OutStream;
    private BufferedReader m_InStream;
    private boolean m_IsConnected;
    private long m_LastClientConnectionTime;

    public PlayerConnection(Socket i_Socket){

        this.m_SocketConnection = i_Socket;
        this.m_IsConnected = true;

        try {
            this.m_OutStream = new PrintWriter(this.m_SocketConnection.getOutputStream(), true);
            this.m_InStream = new BufferedReader(new InputStreamReader(this.m_SocketConnection.getInputStream()));
            this.updateLastClientConnectionTime();

            this.SendMessage(new PlayerGeneralMessage(PlayerGeneralMessage.MessageType.NOTIFICATION, "Connecting to server...").toString());

        }catch(Exception e){
            CloseConnection(e.getMessage());
        }
    }

    public void SendMessage(String i_Message) throws SocketTimeoutException {

        if(isTimedOut())
            throw new SocketTimeoutException(GlobalSettings.TERMINATE_DUE_TO_TIME_OUT);

        try
        {
            String msg = i_Message + "\n";
            this.m_SocketConnection.getOutputStream().write(msg.getBytes());

            this.updateLastClientConnectionTime();
        } catch(IOException ioe) {
            throw new SocketTimeoutException(GlobalSettings.NO_CONNECTION);
        }
    }

    public void CloseConnection(String i_ExceptionMessage)
    {
        if (!this.m_IsConnected)
            return;

        try {
            this.m_IsConnected = false;
            this.m_SocketConnection.close();

            if(!i_ExceptionMessage.equals(GlobalSettings.MATCH_ENDED))
                LoggerManager.error("Socket (" + this.getHost() + "): " + i_ExceptionMessage);

        } catch (Exception e) {
            LoggerManager.error("Socket (" + this.getHost() + "): " + e.getMessage());
        }
    }

    public boolean IsConnectionAlive()
    {
        if (!this.m_IsConnected)
            return false;

        if(System.currentTimeMillis() - this.m_LastClientConnectionTime <= 5000){
           return true;
        }

        try {
            PlayerGeneralMessage heartbeat = new PlayerGeneralMessage(PlayerGeneralMessage.MessageType.CONFIRMATION, GlobalSettings.SERVER_HEARTBEAT_MESSAGE);
            String heartbeatJson = heartbeat.toString() + "\n";

            this.m_SocketConnection.getOutputStream().write(heartbeatJson.getBytes());

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

    public String ReadMessage() throws IOException {

        if(isTimedOut() || !IsConnectionAlive()) {
            throw new SocketTimeoutException(GlobalSettings.TERMINATE_DUE_TO_TIME_OUT);
        }

        String message = null;

        try
        {
            while(this.GetInStream().ready())
            {
                message = this.GetInStream().readLine();

                if(message == null) {
                    throw new IOException(GlobalSettings.TERMINATE_DUE_TO_TIME_OUT);
                }

                // Update the connection time after each received heartbeat
                this.updateLastClientConnectionTime();

                // If the message is not a heartbeat response, break the loop
                if(!message.equals(GlobalSettings.CLIENT_HEARTBEAT_RESPONSE)) {
                    break;
                }
            }

            // If there was no data to read
            if(message == null || message.equals(GlobalSettings.CLIENT_HEARTBEAT_RESPONSE))
            {
                return GlobalSettings.NO_MESSAGES_IN_CLIENT_BUFFER;
            }

            this.updateLastClientConnectionTime();
        }
        catch (IOException e) {
            this.CloseConnection(e.getMessage());
        }

        return message;
    }

    /**
     * @return - Message received by client
     * @throws IOException - When null is read from client's input stream, indicates of connection termination.
     */
    public String WaitForPlayerResponse() throws IOException {
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

    private boolean isTimedOut()
    {
        return (System.currentTimeMillis() - this.m_LastClientConnectionTime > GlobalSettings.MAX_TIME_OUT);
    }

    public String getHost()
    {
        return this.m_SocketConnection.getInetAddress().getHostAddress();
    }

    public BufferedReader GetInStream()
    {
        return m_InStream;
    }

    private void updateLastClientConnectionTime()
    {
        this.m_LastClientConnectionTime = System.currentTimeMillis();
    }
}
