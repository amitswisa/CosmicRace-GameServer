package match_making;

import player.Player;
import dto.PlayerGeneralMessage;
import interfaces.Match;
import utils.loggers.LoggerManager;
import utils.GlobalSettings;

import java.net.SocketTimeoutException;
import java.util.*;

public final class MatchMaking {

    private static final Object incOrDecLock = new Object();
    private static volatile int s_UsersWaiting = 0;
    private static volatile Queue<Player> s_PlayersQueue = new PriorityQueue<>();
    private static volatile Map<String, Match> s_ActiveMatches = new HashMap<>();

    public static synchronized void AddPlayerToWaitingList(Player i_NewPlayer)
    {
        addPlayerToQueue(i_NewPlayer);

        // There are enough users to create a match.
        while (s_UsersWaiting >= GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS) {

            List<Player> matchPlayers = new ArrayList<>(GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS);
            LoggerManager.info("Match Making log: Looking for players...");

            while(matchPlayers.size() < GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS
                    && s_UsersWaiting >= GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS - matchPlayers.size()) {

                Player player = s_PlayersQueue.poll();
                DecreaseUserWaiting();

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

    public static synchronized void RemoveActiveMatch(Match i_Match)
    {
        s_ActiveMatches.remove(i_Match.GetMatchIdentifier());
    }

    private static void addPlayerToQueue(Player i_Player) // Only one sync function call it - no need to sync.
    {
        try {
            s_PlayersQueue.add(i_Player);
            increaseUsersWaiting();
            LoggerManager.info("Player " +i_Player.GetUserName()+ " log: Added to waiting list queue!");

            i_Player.SendMessage(new PlayerGeneralMessage(PlayerGeneralMessage.MessageType.NOTIFICATION, "Looking for other players...").toString());
        } catch(SocketTimeoutException ste) {
            RemovePlayerFromQueue(i_Player);
            i_Player.CloseConnection(ste.getMessage());
        }
    }

    public static void RemovePlayerFromQueue(Player i_Player)
    {
        if(s_PlayersQueue.contains(i_Player))
        {
            s_PlayersQueue.remove(i_Player);
            DecreaseUserWaiting();
        }
    }

    public static void DecreaseUserWaiting() //Ran removed the Synchronized.
    {
        synchronized (incOrDecLock)
        {
            s_UsersWaiting -= 1;
        }
    }

    private static void increaseUsersWaiting() //Ran removed the Synchronized.
    {
        synchronized (incOrDecLock)
        {
            s_UsersWaiting += 1;
        }
    }

    private static String generateSessionIdentifier() {

        Random random = new Random();
        int min = 1000000; // Minimum 7-digit number
        int max = 9999999; // Maximum 7-digit number

        return (random.nextInt(max - min + 1) + min) + "";
    }


}