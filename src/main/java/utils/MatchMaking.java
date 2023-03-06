package utils;

import client.GameSession;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class MatchMaking {

    private static volatile int usersWaiting = 0;
    public static volatile Queue<GameSession> socketManager = new PriorityQueue<>(); // Players waiting for a game to start queue.

    // Adding new socket to socket list.
    public static synchronized void add(GameSession socket) {
        socketManager.add(socket); // Add player to queue.
        increaseUsersWaiting(); // Add user to waiting users count.

        // Loop over the queue to see if there are enough players.
        // Class Match -> hold all relevant players for a single game.

        // There are enough users to create a match.
        if(usersWaiting >= 4) {
            List<GameSession> m_u_List = new ArrayList<>(4);

            // Get 4 users from top of the queue.
            for(int i = 0; i<4;i++)
                m_u_List.add(socketManager.poll());

            Match n_Match = new Match(m_u_List);
        }

        LoggerManager.info("Socket (" +socket.getHost() + "): New connection established!");
    }

    // Removing a socket connection from socket list.
    public static void remove(GameSession socket) {
        socketManager.remove(socket);
    }

    private static void increaseUsersWaiting() {
        usersWaiting += 1;
    }

}
