package match;

import entities.player.HostEntity;
import model.player.PlayerEntity;
import services.OfflineMatchService;

import java.util.*;

public final class OfflineMatchManager
{
    private static volatile Map<String, OfflineMatchService> m_MatchMap = new HashMap<>();


    synchronized private static String generateMatchIdentifier()
    {
        Random random = new Random();
        int identifier = -1;

        do {
            int min = 10000; // Minimum 5-digit number
            int max = 99999; // Maximum 5-digit number

            int tempIdentifier = random.nextInt(max - min + 1) + min;

            if(!m_MatchMap.containsKey(String.valueOf(tempIdentifier)))
            {
                identifier = tempIdentifier;
            }

        } while(identifier == -1);

        return String.valueOf(identifier);
    }

    public static void CreateNewMatchRoom(HostEntity i_HostEntity)
    {
        String matchRoomIdentifier = generateMatchIdentifier();
        List<PlayerEntity> playersList = new ArrayList<>();
        playersList.add(i_HostEntity);

        OfflineMatchService matchService = new OfflineMatchService(i_HostEntity, playersList, matchRoomIdentifier); // Creation of match room.

        // Check if connection to host is still alive.
        if(!matchService.IsGameOver())
        {
            m_MatchMap.put(matchRoomIdentifier, matchService);
        }
    }

    synchronized public static OfflineMatchService GetRoomById(String i_RoomIdentifier)
    {
        return m_MatchMap.get(i_RoomIdentifier);
    }
}
