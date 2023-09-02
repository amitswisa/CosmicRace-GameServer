package factories;

import jakarta.websocket.Session;
import match.MatchMaking;
import match.OfflineMatchManager;
import entities.player.HostEntity;
import entities.player.PCPlayerEntity;
import entities.connection.PCConnection;
import entities.connection.WebConnection;
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
                    HostEntity hostEntity = new HostEntity(newConnection);
                    OfflineMatchManager.CreateNewMatchRoom(hostEntity);
                }
                else
                {
                    PCPlayerEntity playerEntity = new PCPlayerEntity(newConnection);
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
            PCPlayerEntity newMatchPCPlayerEntity = null;
            try{
                newMatchPCPlayerEntity = new PCPlayerEntity(new WebConnection(newSessionConnection));

            } catch(IOException e) {
                LoggerManager.error(e.getMessage());
            }
        });

        newPlayerCreationThread.start();
    }

}
