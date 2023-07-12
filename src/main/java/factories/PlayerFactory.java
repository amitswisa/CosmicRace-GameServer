package factories;

import jakarta.websocket.Session;
import match_making.MatchMaking;
import player.Player;
import player.connection_handler.PCConnection;
import player.connection_handler.WebConnection;
import utils.loggers.LoggerManager;

import java.io.IOException;
import java.net.Socket;

public class PlayerFactory {
    public static void createNewPlayer(Socket newSocketConnection) {

        Thread newPlayerCreationThread = new Thread(() -> {
            Player newPlayer = null;
            try{
                newPlayer = new Player(new PCConnection(newSocketConnection));
                MatchMaking.AddPlayerToWaitingList(newPlayer);
            } catch(IOException e) {
                LoggerManager.error(e.getMessage());
            }
        });

        newPlayerCreationThread.start();

    }

    public static void createNewPlayer(Session newSessionConnection) {

        Thread newPlayerCreationThread = new Thread(() -> {
            Player newPlayer = null;
            try{
                newPlayer = new Player(new WebConnection(newSessionConnection));
                MatchMaking.AddPlayerToWaitingList(newPlayer);
            } catch(IOException e) {
                LoggerManager.error(e.getMessage());
            }
        });

        newPlayerCreationThread.start();

    }

}
