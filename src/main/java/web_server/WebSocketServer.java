package web_server;


import factories.PlayerFactory;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ServerEndpoint("/")
public class WebSocketServer {

    @OnOpen
    public void onOpen(Session session) {
        // Handle WebSocket connection open event
        System.out.println("WebSocket connection opened: " + session.getId());
        PlayerFactory.createNewPlayer(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // Handle WebSocket message received event
        System.out.println("Received message from " + session.getId() + ": " + message);

        // Echo the message back to the client
        try {
            session.getBasicRemote().sendText("Echo: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        // Handle WebSocket connection close event
        System.out.println("WebSocket connection closed: " + session.getId() + ", Reason: " + reason.getReasonPhrase());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Handle WebSocket error event
        throwable.printStackTrace();
    }


    public static void startWebServer(){
        Map<String, Object> map = new HashMap<>();
        int port = 8081;

        org.glassfish.tyrus.server.Server server = new
                org.glassfish.tyrus.server.Server("localhost", port, "/",
                map, WebSocketServer.class);

        try {
            server.start();
            System.out.println("WebSocket server started. Listening on: " + server.getPort()); //was written getURI()

            // Keep the server running until interrupted
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }

    public static void main(String[] args) {
        startWebServer();
    }
}