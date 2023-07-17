package servers;

import factories.PlayerFactory;
import utils.loggers.LoggerManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class GameSocketServer extends Thread
{
    private static final int PORT = 6666;
    private ServerSocket m_ServerSocket;

    public GameSocketServer()
    {
        try {
            this.m_ServerSocket = new ServerSocket(PORT);
            LoggerManager.info("Game server is up and listens on port: " + PORT);
        } catch (IOException e) {
            LoggerManager.error(e.getMessage());
        }
    }

    @Override
    public void run()
    {
        while(true)
        {
            try {
                Socket newSocketConnection = this.m_ServerSocket.accept();
                PlayerFactory.CreateNewPlayer(newSocketConnection);
            } catch (IOException e) {
                LoggerManager.error(e.getMessage());
            }
        }
    }

}
