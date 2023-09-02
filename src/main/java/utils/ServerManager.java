package utils;

import servers.GameSocketServer;
import servers.WebSocketServer;

public final class ServerManager
{
    private final WebSocketServer m_WebServer;
    private final GameSocketServer m_GameServer;

    public ServerManager()
    {
        this.m_WebServer = new WebSocketServer();
        this.m_GameServer = new GameSocketServer();
    }

    public void Start()
    {
        this.m_GameServer.start(); // Starting game server thread.
        this.m_WebServer.start(); // Starting web connection's listener.
    }
}
