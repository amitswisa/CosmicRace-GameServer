package servers;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import entities.player.MatchWebPlayerEntity;
import entities.connection.WebConnection;
import utils.loggers.LoggerManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/")
public class WebSocketServer extends Thread {
    private static final int PORT = 8081;
    private static final Map<String, MatchWebPlayerEntity> s_WebSocketsPlayerMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        // Handle WebSocket connection open event
        System.out.println("WebSocket connection opened: " + session.getId());

        // Create a new instance of MyWebSocket and add it to the collection
        MatchWebPlayerEntity webPlayer = new MatchWebPlayerEntity(new WebConnection(session));
        s_WebSocketsPlayerMap.put(session.getId(), webPlayer);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // Delegate message handling to the MyWebSocket instance associated with this session
        MatchWebPlayerEntity webPlayer = getWebPlayerEntity(session);

        if (webPlayer != null)
        {
            webPlayer.HandleMessageReceived(message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        // Handle WebSocket connection close event
        MatchWebPlayerEntity webPlayer = getWebPlayerEntity(session);

        if(webPlayer != null)
        {
            webPlayer.CloseConnection(reason.getReasonPhrase());
        }

        s_WebSocketsPlayerMap.remove(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Handle WebSocket error event
        onClose(session, new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, throwable.getMessage()));
    }

    @Override
    public void run() {
        Map<String, Object> map = new HashMap<>();

        org.glassfish.tyrus.server.Server server = new
                org.glassfish.tyrus.server.Server("localhost", PORT, "/offlinegame",
                map, WebSocketServer.class);

        try {
            server.start();
            LoggerManager.info("WebSocket server started. Listening on: " + server.getPort()); //was written getURI()

            // Keep the server running until interrupted
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }

    private MatchWebPlayerEntity getWebPlayerEntity(Session session) {
        return s_WebSocketsPlayerMap.get(session.getId());
    }

}