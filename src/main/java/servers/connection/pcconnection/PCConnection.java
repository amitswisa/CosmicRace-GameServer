package servers.connection.pcconnection;

import com.google.gson.JsonObject;
import dto.ServerGeneralMessage;
import servers.connection.Connection;
import utils.GlobalSettings;
import utils.json.JsonFormatter;
import utils.loggers.LoggerManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class PCConnection extends Connection {

    private final Socket m_SocketConnection;
    private PrintWriter m_OutStream;
    private BufferedReader m_InStream;
    private boolean m_IsHost;

    public PCConnection(Socket i_Socket)
    {
        this.m_SocketConnection = i_Socket;

        try {
            // Set & Get stream buffers.
            this.m_OutStream = new PrintWriter(this.m_SocketConnection.getOutputStream(), true);
            this.m_InStream = new BufferedReader(new InputStreamReader(this.m_SocketConnection.getInputStream()));

            // Notification to client.
            this.SendMessage(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, "Connecting to server...").toString());

            // Check if received connection is host or player.
            String initPlayerConfiguration = this.WaitForPlayerResponse();
            JsonObject initDataJson = JsonFormatter.createJsonFromString(initPlayerConfiguration);

            this.m_IsHost = initDataJson.get("gameType").getAsString().equals("Offline");

        } catch(Exception e) {
            CloseConnection(e.getMessage());
        }
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

    public void SendMessage(String i_Message) throws SocketTimeoutException
    {

        if(isTimedOut())
            throw new SocketTimeoutException(GlobalSettings.TERMINATE_DUE_TO_TIME_OUT);

        try
        {
            String msg = i_Message + "\n";
            this.m_SocketConnection.getOutputStream().write(msg.getBytes());

            this.UpdateLastConnectionTime();
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

        if(!ValidateConnectionNeeded()){
           return true;
        }

        try {
            ServerGeneralMessage heartbeat = new ServerGeneralMessage(ServerGeneralMessage.eActionType.CONFIRMATION, GlobalSettings.SERVER_HEARTBEAT_MESSAGE);
            String heartbeatJson = heartbeat.toString() + "\n";

            this.m_SocketConnection.getOutputStream().write(heartbeatJson.getBytes());

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

    public String ReadMessage() throws IOException
    {

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
                this.UpdateLastConnectionTime();

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

            this.UpdateLastConnectionTime();
        }
        catch (IOException e) {
            this.CloseConnection(e.getMessage());
        }

        return message;
    }

    public String getHost()
    {
        return this.m_SocketConnection.getInetAddress().getHostAddress();
    }

    public BufferedReader GetInStream()
    {
        return m_InStream;
    }

    public boolean IsHostConnection()
    {
        return this.m_IsHost;
    }
}
