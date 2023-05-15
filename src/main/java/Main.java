import factories.PlayerFactory;
import player.Player;
import match_making.MatchMaking;
import utils.GlobalSettings;
import utils.logs.LoggerManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws Exception {

        ServerSocket serverSocket = new ServerSocket(GlobalSettings.PORT);

        while(true) {
            Socket newSocketConnection = serverSocket.accept();
            PlayerFactory.createNewPlayer(newSocketConnection);
        }
    }
}