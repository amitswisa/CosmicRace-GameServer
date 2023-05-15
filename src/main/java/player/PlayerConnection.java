package player;

import dto.ClientMessage;
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
            handleClientError(e);
        }
    }

    private void updateLastClientConnectionTime()
    {
        this.m_LastClientConnectionTime = System.currentTimeMillis();
    }

    private void handleClientError(Exception e)
    {
        sendMessage(new ClientMessage(ClientMessage.MessageType.ERROR, e.getMessage()).toString());
        closeConnection();
    }

    public final void sendMessage(String message)
    {
        m_OutStream.println(message);
    }

    public final void closeConnection()
    {
        if (!this.m_IsConnected || this.m_SocketConnection.isClosed())
            return;

        try {
            this.m_IsConnected = false;
            LoggerManager.info("Socket (" + this.getHost() + "): Connection closed!");

            this.m_SocketConnection.close();
        } catch (Exception e) {
            LoggerManager.error(e.getMessage());
        }
    }

    public final String getHost()
    {
        return this.m_SocketConnection.getInetAddress().getHostAddress();
    }

    public final boolean IsConnectionAlive()
    {
        try {
            ClientMessage heartbeat = new ClientMessage(ClientMessage.MessageType.CONFIRMATION, "isAlive\n");
            String heartbeatReadyToSend = heartbeat.toString() + "\n";
            this.m_SocketConnection.getOutputStream().write(heartbeatReadyToSend.getBytes());
        } catch (Exception e) {
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

    public final String ReadMessage()
    {
        try {
            String msg = GetInStream().readLine();
            return msg;
        } catch (IOException e) {

            //TODO - handle player client termination.
            return null;
        }
    }

    public String WaitForPlayerResponse(){
        try{
            // Get initialization data from client in json (contains userid & characterId).
            String initData = m_InStream.readLine();
            return initData;
        } catch (Exception e) {
            LoggerManager.error("Error occurred with " + this.getHost() + ": " + e.getMessage());
            handleClientError(e);
        }
        return null;
    }
}
