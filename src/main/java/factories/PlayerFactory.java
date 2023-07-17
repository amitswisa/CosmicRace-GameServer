package factories;

import jakarta.websocket.Session;
import match.MatchMaking;
import match.OfflineMatchManager;
import entities.player.MatchHostEntity;
import entities.player.MatchPCPlayerEntity;
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
                    MatchHostEntity hostEntity = new MatchHostEntity(newConnection);
                    OfflineMatchManager.CreateNewMatchRoom(hostEntity);
                }
                else
                {
                    MatchPCPlayerEntity playerEntity = new MatchPCPlayerEntity(newConnection);
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
            MatchPCPlayerEntity newMatchPCPlayerEntity = null;
            try{
                newMatchPCPlayerEntity = new MatchPCPlayerEntity(new WebConnection(newSessionConnection));

            } catch(IOException e) {
                LoggerManager.error(e.getMessage());
            }
        });

        newPlayerCreationThread.start();
    }

}
