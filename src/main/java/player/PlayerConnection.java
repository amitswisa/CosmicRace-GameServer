package player;

import dto.ClientMessage;
import utils.GlobalSettings;
import utils.logs.LoggerManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public final class PlayerConnection {

    private final Socket m_SocketConnection; // Client's socket.
    private PrintWriter m_OutStream; // Output stream.
    private BufferedReader m_InStream; // Input stream.
    private boolean m_IsConnected;
    private long m_LastClientConnectionTime;

    public PlayerConnection(Socket i_Socket){

        this.m_SocketConnection = i_Socket;
        this.m_IsConnected = true;

        try {
            this.m_OutStream = new PrintWriter(this.m_SocketConnection.getOutputStream(), true);
            this.m_InStream = new BufferedReader(new InputStreamReader(this.m_SocketConnection.getInputStream()));
            this.updateLastClientConnectionTime();

            this.sendMessage(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Connection established.").toString());

        }catch(Exception e){
            CloseConnection(e.getMessage());
        }
    }

    private void updateLastClientConnectionTime()
    {
        this.m_LastClientConnectionTime = System.currentTimeMillis();
    }

    public void sendMessage(String message)
    {
        if(this.IsConnectionAlive())
            m_OutStream.println(message);
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

    public final String getHost()
    {
        return this.m_SocketConnection.getInetAddress().getHostAddress();
    }

    public boolean IsConnectionAlive()
    {
        if (!this.m_IsConnected)
            return false;

        try {
            ClientMessage heartbeat = new ClientMessage(ClientMessage.MessageType.CONFIRMATION, "isAlive\n");
            String heartbeatReadyToSend = heartbeat.toString() + "\n";
            this.m_SocketConnection.getOutputStream().write(heartbeatReadyToSend.getBytes());
            this.updateLastClientConnectionTime();
        } catch (Exception e) {

            // Client timed out.
            if(isTimedOut())
            {
                CloseConnection(e.getMessage());
            }

            return false;
        }

        return true;
    }

    public final PrintWriter GetOutStream()
    {
        return m_OutStream;
    }

    public final BufferedReader GetInStream()
    {
        return m_InStream;
    }

    public String ReadMessage() throws IOException {
        String lastClientMessage = "";
        do
        {
            if(this.GetInStream().ready())
            {
                lastClientMessage = this.GetInStream().readLine();

                // TODO - Cheeck
                if(lastClientMessage == null)
                    throw new IOException(GlobalSettings.TERMINATE_DUE_TO_TIME_OUT);

                this.updateLastClientConnectionTime();
            }
            else {
                return GlobalSettings.NO_MESSAGES_IN_CLIENT_BUFFER;
            }

        } while(lastClientMessage.equals(GlobalSettings.CLIENT_HEARTBEAT_RESPONSE));

        return lastClientMessage;
    }

    public String WaitForPlayerResponse()
    {
        while(!isTimedOut())
        {
            try{
                // Get initialization data from client in json (contains userid & characterId).
                String initData = this.ReadMessage();

                if(initData.equals(GlobalSettings.NO_MESSAGES_IN_CLIENT_BUFFER))
                    continue;

                return initData;

            } catch (Exception e) {
                this.CloseConnection(e.getMessage());
                break;
            }
        }

        return null;
    }

    private boolean isTimedOut()
    {
        return (System.currentTimeMillis() - this.m_LastClientConnectionTime > GlobalSettings.MAX_TIME_OUT);
    }
}
