package utils;

import client.GameSession;

import java.util.PriorityQueue;
import java.util.Queue;

public class MatchMaking {

    public static volatile Queue<GameSession> socketManager = new PriorityQueue<>(); // Players waiting for a game to start queue.
    public static volatile int SESSION_ID = 1;

    // Sync locks.
    private static final Object broadcasstLocker = new Object();

    // Adding new socket to socket list.
    public static void add(GameSession socket) {
        socketManager.add(socket); // Add player to queue.
        socket.setSESSION_ID(allocateSessionId()); // Create socket's unique id.

        // Loop over the queue to see if there are enough players.
        // Class Match -> hold all relevant players for a single game.

        LoggerManager.info("Socket (" +socket.getHost()+ " #" +socket.getSESSION_ID()+ "): New connection established!");
    }

    // Removing a socket connection from socket list.
    public static void remove(GameSession socket) {
        socketManager.remove(socket);
    }



    public static int allocateSessionId() {
        SESSION_ID += 1;
        return SESSION_ID - 1;
    }
}
