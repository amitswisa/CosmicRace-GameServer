package match_making;

import player.Player;
import dto.ClientMessage;
import interfaces.Match;
import utils.logs.LoggerManager;
import utils.GlobalSettings;

import java.net.SocketTimeoutException;
import java.util.*;

public final class MatchMaking {

    private static final Object removeLock = new Object();
    private static volatile int s_UsersWaiting = 0;
    private static volatile Queue<Player> s_PlayersQueue = new PriorityQueue<>();
    private static volatile Map<String, Match> s_ActiveMatches = new HashMap<>();

    public static synchronized void AddPlayerToWaitingList(Player i_NewPlayer)
    {

        try {
            i_NewPlayer.SendMessage(new ClientMessage(ClientMessage.MessageType.NOTIFICATION, "Looking for other players...").toString());
            addPlayerToQueue(i_NewPlayer);
            LoggerManager.info("Player " +i_NewPlayer.GetUserName()+ " log: Added to waiting list queue!");
        } catch(SocketTimeoutException ste) {
            i_NewPlayer.CloseConnection(ste.getMessage());
        }

        // There are enough users to create a match.
        while (s_UsersWaiting >= GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS) {

            List<Player> matchPlayers = new ArrayList<>(GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS);
            LoggerManager.info("Match Making log: Looking for players...");

            while(matchPlayers.size() < GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS
                    && s_UsersWaiting >= GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS - matchPlayers.size()) {

                Player player = s_PlayersQueue.poll();
                DecreaseUserWaiting();

                // TODO - Recheck player terminated connection or has lagging.
                if (player != null && player.IsConnectionAlive())
                    matchPlayers.add(player);
                else
                    LoggerManager.info("Player " +player.GetUserName()+ " log: Connection terminated or unreachable.");
            }

            int numOfPlayersInList = matchPlayers.size();
            if(numOfPlayersInList != GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS)
            {
                LoggerManager.info("Match Making log: Failed to create a match, not enough players.");
                s_PlayersQueue.addAll(matchPlayers);
                s_UsersWaiting += numOfPlayersInList;
                return;
            }

            // Create new match and start it.
            String matchIdentifier = generateSessionIdentifier();
            LoggerManager.info("Match Making log: Players found, creating a match (#"+matchIdentifier+").");

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

    public static synchronized void RemoveActiveMatch(Match i_Match)
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