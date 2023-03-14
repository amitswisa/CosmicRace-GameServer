package utils;

import client.Player;

import java.nio.charset.Charset;
import java.util.*;

public class MatchMaking {

    private static final Object  removeLock = new Object();
    private static volatile int usersWaiting = 0;
    public static volatile Queue<Player> socketManager = new PriorityQueue<>(); // Players waiting for a game to start queue.
    public static volatile Map<String, Match> activeMatches = new HashMap<>();

    // Adding new socket to socket list.
    public static synchronized void addPlayerToWaitingList(Player socket) throws InterruptedException {
        socketManager.add(socket); // Add player to queue.
        increaseUsersWaiting(); // Add user to waiting users count.
        LoggerManager.info("Socket (" +socket.getHost() + "): Waiting to play!");

        // There are enough users to create a match.
        if(usersWaiting >= Utils.MAXIMUM_AMOUNT_OF_PLAYERS) {

            System.out.println("Server is loading....");

            List<Player> m_u_List = new ArrayList<>(4);

            // Get 4 users from top of the queue.
            for(int i = 0; i < Utils.MAXIMUM_AMOUNT_OF_PLAYERS; i++)
            {
                Player player = socketManager.poll();

                // If at least one player quits from waiting for a match and there aren't enough players.
                if(player == null)
                {
                    // TODO: Notify to all other players - CHECK.
                    socketManager.addAll(m_u_List);
                    return;
                }

                m_u_List.add(player);
            }

            // Create new match and start it.
            String matchIdentifyer = generateSessionIdentifyer();
            Match newMatch = new Match(matchIdentifyer, m_u_List);
            activeMatches.put(matchIdentifyer, newMatch);

            // Log players currently playing.
            LoggerManager.info("New match! Players: ");
            m_u_List.forEach(player -> LoggerManager.info(player.getPlayerName()));

            newMatch.start();
        }

    }

    // Removing a socket connection from socket list.
    public static void removePlayerFromWaitingList(Player socket) {
        synchronized (removeLock) {
            socketManager.remove(socket);
            decreaseUserWaiting();
        }
    }

    public static void removeActiveMatch(Match match) {
        activeMatches.remove(match.getIdentifyer());
    }

    public static void decreaseUserWaiting(){
        --usersWaiting;
    }
    private static void increaseUsersWaiting() {
        usersWaiting += 1;
    }

    private static String generateSessionIdentifyer() {
        String generatedString;

        do {
            byte[] bytes = new byte[7];
            new Random().nextBytes(bytes);
            generatedString = new String(bytes, Charset.forName("UTF-8"));
        } while(activeMatches.containsKey(generatedString));

        return generatedString;

    }

}
