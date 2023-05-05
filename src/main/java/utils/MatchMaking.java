package utils;

import client.Player;
import interfaces.Match;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MatchMaking {
    private static final Object  removeLock = new Object();
    private static final Object  customRemoveLock = new Object(); //not a good name for this variable.
    private static volatile int usersWaiting = 0;
    private static volatile boolean serverRunning;
    public static volatile Queue<Player> playersManager = new PriorityQueue<>(); // Players waiting for a game to start queue.
    public static volatile Map<String, Match> activeMatches = new HashMap<>();

    // Adding new socket to socket list.
    public static synchronized void addPlayerToWaitingList(Player newPlayer) throws InterruptedException {

        playersManager.add(newPlayer); // Add player to queue.
        increaseUsersWaiting(); // Add user to waiting users count.

        newPlayer.sendMessage("N: Looking for other players...");
        LoggerManager.info("Socket (" +newPlayer.getHost() + "): Waiting to play!");

        // There are enough users to create a match.
        while (usersWaiting >= Utils.MAXIMUM_AMOUNT_OF_PLAYERS) {

            LoggerManager.info("Try to start new match...");

            List<Player> m_u_List = new ArrayList<>(Utils.MAXIMUM_AMOUNT_OF_PLAYERS);

            // Get 4 users from top of the queue.
            LoggerManager.debug("Size of list: " + m_u_List.size());
            while(m_u_List.size() < Utils.MAXIMUM_AMOUNT_OF_PLAYERS
                    && usersWaiting >= Utils.MAXIMUM_AMOUNT_OF_PLAYERS - m_u_List.size()) {
                Player player = playersManager.poll();

                // If at least one player quits from waiting for a match and there aren't enough players.
                if (player != null && player.isConnectionAlive()){
                    m_u_List.add(player);
                    decreaseUserWaiting();
                }
                else
                    LoggerManager.info("player removed from Player's Queue (quit from game).");

            }

            int numOfPlayersInList = m_u_List.size();
            if(numOfPlayersInList != Utils.MAXIMUM_AMOUNT_OF_PLAYERS)
            {
                LoggerManager.info("not enough players to create a game. exiting.");
                playersManager.addAll(m_u_List);
                usersWaiting += numOfPlayersInList;
                return;
            }

            // Create new match and start it.
            String matchIdentifyer = generateSessionIdentifyer();
            Match newMatch = new MultiPlayerMatch(matchIdentifyer, m_u_List);
            activeMatches.put(matchIdentifyer, newMatch);

            // Log players currently playing.
            LoggerManager.info("New match! Players: ");
            m_u_List.forEach(player -> LoggerManager.info(player.getPlayerName()));

            new Thread((Runnable) newMatch).start();
        }
    }

    // Removing a socket connection from socket list.
    public static void removePlayerFromWaitingList(Player socket) {
        synchronized (removeLock) {
            playersManager.remove(socket);
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
            generatedString = new String(bytes, StandardCharsets.UTF_8);
        } while(activeMatches.containsKey(generatedString));

        return generatedString;

    }

    public static void serverRunning(boolean value) {
        serverRunning = value;
    }
}

/*
for (Map.Entry<String, List<Player>> entry : waitingRoomList.entrySet()) {
        for (Player player:entry.getValue()) {
        if(player.getSocketConnection().isClosed()){
        player.closeConnection();
        }
        }
        try {
        Thread.sleep(500);
        } catch (InterruptedException e) {
        throw new RuntimeException(e);
        }
        }
*/
