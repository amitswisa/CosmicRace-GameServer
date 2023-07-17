package factories;

import match.entities.match_host.MatchHostEntity;
import jakarta.websocket.Session;
import match.MatchMaking;
import match.OfflineMatchManager;
import match.entities.match_player.MatchPlayerEntity;
import servers.connection.pcconnection.PCConnection;
import utils.loggers.LoggerManager;

import java.io.IOException;
import java.net.Socket;

public class PlayerFactory {

    public static void CreateNewPlayer(Socket newSocketConnection)
    {
        Thread connectionSetupThread = new Thread(() -> {

            try
            {
                PCConnection newConnection = new PCConnection(newSocketConnection);

                if(newConnection.IsHostConnection())
                {
                    MatchHostEntity hostEntity = new MatchHostEntity(newConnection);
                    OfflineMatchManager.CreateNewMatchRoom(hostEntity);
                }
                else
                {
                    MatchPlayerEntity playerEntity = new MatchPlayerEntity(newConnection);
                    MatchMaking.AddPlayerToWaitingList(playerEntity);
                }

            } catch(IOException e) {
                LoggerManager.error(e.getMessage());
            }

        });

        connectionSetupThread.start();
    }

    public static void CreateNewPlayer(Session newSessionConnection)
    {
        Thread newPlayerCreationThread = new Thread(() -> {
            /*MatchPlayerEntity newMatchPlayerEntity = null;
            try{
                newMatchPlayerEntity = new MatchPlayerEntity(new WebConnection(newSessionConnection));
                MatchMaking.AddPlayerToWaitingList(newMatchPlayerEntity);
            } catch(IOException e) {
                LoggerManager.error(e.getMessage());
            }*/
        });

        newPlayerCreationThread.start();
    }

}
