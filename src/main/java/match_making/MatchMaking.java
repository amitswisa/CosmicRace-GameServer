package match_making;

import player.Player;
import dto.ClientMessage;
import interfaces.Match;
import utils.LoggerManager;
import utils.Utils;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MatchMaking {

    private static final Object  removeLock = new Object();
    private static final Object  customRemoveLock = new Object(); //not a good name for this variable.
    private static volatile int usersWaiting = 0;
    private static volatile Queue<Player> playersManager = new PriorityQueue<>(); // Players waiting for a game to start queue.
    private static volatile Map<String, Match> activeMatches = new HashMap<>();

    public static synchronized void addPlayerToWaitingList(Player newPlayer) {

        addPlayerToQueue(newPlayer);
        newPlayer.sendMessage(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Looking for other players...").toString());
        LoggerManager.info("Socket (" +newPlayer.getHost() + "): Waiting to play!");

        // There are enough users to create a match.
        while (usersWaiting >= Utils.MAXIMUM_AMOUNT_OF_PLAYERS) {

            List<Player> matchPlayers = new ArrayList<>(Utils.MAXIMUM_AMOUNT_OF_PLAYERS);
            LoggerManager.info("Try to start new match...");

            while(matchPlayers.size() < Utils.MAXIMUM_AMOUNT_OF_PLAYERS
                    && usersWaiting >= Utils.MAXIMUM_AMOUNT_OF_PLAYERS - matchPlayers.size()) {

                Player player = playersManager.poll();

                // If at least one player quits from waiting for a match and there aren't enough players.
                if (player != null && player.isConnectionAlive()){
                    matchPlayers.add(player);
                    decreaseUserWaiting();
                }
                else
                    LoggerManager.info("player removed from Player's Queue (quit from game).");

            }

            int numOfPlayersInList = matchPlayers.size();
            if(numOfPlayersInList != Utils.MAXIMUM_AMOUNT_OF_PLAYERS)
            {
                LoggerManager.info("not enough players to create a game. exiting.");
                playersManager.addAll(matchPlayers);
                usersWaiting += numOfPlayersInList;
                return;
            }

            // Create new match and start it.
            String matchIdentifyer = generateSessionIdentifyer();
            Match newMatch = new OnlineMatch(matchIdentifyer, matchPlayers);
            activeMatches.put(matchIdentifyer, newMatch);



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
        activeMatches.remove(match.getM_MatchIdentifier());
    }

    public static synchronized void decreaseUserWaiting(){
        --usersWaiting;
    }

    private synchronized static void addPlayerToQueue(Player player) {
        playersManager.add(player); // Add player to queue.
        increaseUsersWaiting(); // Add user to waiting users count.
    }

    private static synchronized void increaseUsersWaiting() {
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


}