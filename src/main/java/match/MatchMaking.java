package match;

import model.player.PlayerEntity;
import services.OnlineMatchService;
import entities.player.PCPlayerEntity;
import dto.ServerGeneralMessage;
import utils.loggers.LoggerManager;
import utils.GlobalSettings;
import java.net.SocketTimeoutException;
import java.util.*;

public final class MatchMaking
{

    private static final Object incOrDecLock = new Object();
    private static volatile int s_UsersWaiting = 0;
    private static volatile Queue<PlayerEntity> s_PlayersQueue = new PriorityQueue<>();
    private static volatile Map<String, OnlineMatchService> s_ActiveMatches = new HashMap<>();

    public static synchronized void AddPlayerToWaitingList(PCPlayerEntity i_NewPlayerEntity)
    {
        addPlayerToQueue(i_NewPlayerEntity);

        // There are enough users to create a match.
        while (s_UsersWaiting >= GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS) {

            List<PlayerEntity> matchPlayersList = new ArrayList<>(GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS);
            LoggerManager.info("Match Making log: Looking for players...");

            while(matchPlayersList.size() < GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS
                    && s_UsersWaiting >= GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS - matchPlayersList.size()) {

                PlayerEntity matchPlayer = s_PlayersQueue.poll();
                DecreaseUserWaiting();

                if (matchPlayer != null && matchPlayer.IsConnectionAlive())
                    matchPlayersList.add(matchPlayer);
                else
                    LoggerManager.info("Player " + matchPlayer.GetUserName()+ " log: Connection terminated or unreachable.");
            }

            int numOfPlayersInList = matchPlayersList.size();
            if(numOfPlayersInList != GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS)
            {
                LoggerManager.info("Match Making log: Failed to create a match, not enough players.");
                s_PlayersQueue.addAll(matchPlayersList);
                s_UsersWaiting += numOfPlayersInList;
                return;
            }

            // Create new match and start it.
            String matchIdentifier = generateSessionIdentifier();
            LoggerManager.info("Match Making log: Players found, creating a match (#"+matchIdentifier+").");

            OnlineMatchService newMatch = new OnlineMatchService(matchIdentifier, matchPlayersList);
            s_ActiveMatches.put(matchIdentifier, newMatch);

            newMatch.start(); // Start Match.
        }
    }

    public static synchronized void RemoveActiveMatch(OnlineMatchService i_Match)
    {
        s_ActiveMatches.remove(i_Match.GetMatchIdentifier());
    }

    // Only one sync function call it - no need to sync.
    private static void addPlayerToQueue(PlayerEntity i_Match_PlayerEntity)
    {
        try {
            s_PlayersQueue.add(i_Match_PlayerEntity);
            increaseUsersWaiting();
            LoggerManager.info("Player " + i_Match_PlayerEntity.GetUserName()+ " log: Added to waiting list queue!");

            i_Match_PlayerEntity.SendMessage(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, "Looking for other players...").toString());
        } catch(SocketTimeoutException ste) {
            RemovePlayerFromQueue(i_Match_PlayerEntity);
            i_Match_PlayerEntity.CloseConnection(ste.getMessage());
        }
    }

    public static void RemovePlayerFromQueue(PlayerEntity i_Match_PlayerEntity)
    {
        if(s_PlayersQueue.contains(i_Match_PlayerEntity))
        {
            s_PlayersQueue.remove(i_Match_PlayerEntity);
            DecreaseUserWaiting();
        }
    }

    public static void DecreaseUserWaiting()
    {
        synchronized (incOrDecLock)
        {
            s_UsersWaiting -= 1;
        }
    }

    private static void increaseUsersWaiting()
    {
        synchronized (incOrDecLock)
        {
            s_UsersWaiting += 1;
        }
    }

    private static String generateSessionIdentifier()
    {

        Random random = new Random();
        int min = 1000000; // Minimum 7-digit number
        int max = 9999999; // Maximum 7-digit number

        return (random.nextInt(max - min + 1) + min) + "";
    }

}