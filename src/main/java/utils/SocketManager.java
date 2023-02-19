package utils;

import client.GameSession;

import java.util.HashSet;
import java.util.Set;

public class SocketManager {
    public static volatile Set<GameSession> socketManager = new HashSet<>();
    public static volatile int SESSION_ID = 1;

    // Sync locks.
    private static final Object broadcasstLocker = new Object();

    // Adding new socket to socket list.
    public static void add(GameSession socket) {
        socketManager.add(socket);
        socket.setSESSION_ID(allocateSessionId()); // Create socket's unique id.
        LoggerManager.info("Socket (" +socket.getHost()+ " #" +socket.getSESSION_ID()+ "): New connection established!");
    }

    // Removing a socket connection from socket list.
    public static void remove(GameSession socket) {
        socketManager.remove(socket);
    }

    // Send message to every socket connected to the server.
    public static void broadcast(String message) {
        synchronized (broadcasstLocker) {
            socketManager.stream().forEach((session) -> {
                session.send(message);
            });
        }
    }

    public static int allocateSessionId() {
        SESSION_ID += 1;
        return SESSION_ID - 1;
    }
}
