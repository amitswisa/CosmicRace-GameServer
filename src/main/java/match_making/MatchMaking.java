package match_making;

import player.Player;
import dto.ClientMessage;
import interfaces.Match;
import utils.logs.LoggerManager;
import utils.Utils;

import java.util.*;

public final class MatchMaking {

    private static final Object removeLock = new Object();
    private static final Object customRemoveLock = new Object(); //not a good name for this variable.
    private static volatile int s_UsersWaiting = 0;
    private static volatile Queue<Player> s_PlayersQueue = new PriorityQueue<>();
    private static volatile Map<String, Match> s_ActiveMatches = new HashMap<>();

    public static synchronized void AddPlayerToWaitingList(Player i_NewPlayer)
    {

        addPlayerToQueue(i_NewPlayer);
        i_NewPlayer.sendMessage(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Looking for other players...").toString());
        LoggerManager.info("Socket (" +i_NewPlayer.getHost() + "): Waiting to play!");

        // There are enough users to create a match.
        while (s_UsersWaiting >= Utils.MAXIMUM_AMOUNT_OF_PLAYERS) {

            List<Player> matchPlayers = new ArrayList<>(Utils.MAXIMUM_AMOUNT_OF_PLAYERS);
            LoggerManager.info("Try to start new match...");

            while(matchPlayers.size() < Utils.MAXIMUM_AMOUNT_OF_PLAYERS
                    && s_UsersWaiting >= Utils.MAXIMUM_AMOUNT_OF_PLAYERS - matchPlayers.size()) {

                Player player = s_PlayersQueue.poll();
                DecreaseUserWaiting();

                // If at least one player quits from waiting for a match and there aren't enough players.
                if (player != null && player.IsConnectionAlive())
                    matchPlayers.add(player);
                else
                    LoggerManager.info("player removed from Player's Queue (quit from game).");

            }

            int numOfPlayersInList = matchPlayers.size();
            if(numOfPlayersInList != Utils.MAXIMUM_AMOUNT_OF_PLAYERS)
            {
                LoggerManager.info("not enough players to create a game. exiting.");
                s_PlayersQueue.addAll(matchPlayers);
                s_UsersWaiting += numOfPlayersInList;
                return;
            }

            // Create new match and start it.
            String matchIdentifier = generateSessionIdentifier();
            Match newMatch = new OnlineMatch(matchIdentifier, matchPlayers);
            s_ActiveMatches.put(matchIdentifier, newMatch);

            new Thread((Runnable) newMatch).start();
        }
    }

    public static void RemovePlayerFromWaitingList(Player i_PlayerSocket)
    {
        synchronized (removeLock) {
            s_PlayersQueue.remove(i_PlayerSocket);
            DecreaseUserWaiting();
        }
    }

    public static void RemoveActiveMatch(Match i_Match)
    {
        s_ActiveMatches.remove(i_Match.GetMatchIdentifier());
    }

    public static synchronized void DecreaseUserWaiting()
    {
        --s_UsersWaiting;
    }

    private synchronized static void addPlayerToQueue(Player i_Player)
    {
        s_PlayersQueue.add(i_Player); // Add player to queue.
        increaseUsersWaiting(); // Add user to waiting users count.
    }

    private static synchronized void increaseUsersWaiting()
    {
        s_UsersWaiting += 1;
    }

    private static String generateSessionIdentifier() {

        Random random = new Random();
        int min = 1000000; // Minimum 7-digit number
        int max = 9999999; // Maximum 7-digit number

        return (random.nextInt(max - min + 1) + min) + "";
    }


}