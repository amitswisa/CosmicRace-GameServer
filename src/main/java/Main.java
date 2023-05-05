import client.Player;
import interfaces.Match;
import utils.MatchMaking;
import utils.Utils;
import client.OnlinePlayer;
import utils.factories.PlayerFactory;

import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws Exception {

        // Create server socket listening on port 6666;
        ServerSocket serverSocket = new ServerSocket(Utils.PORT);

        MatchMaking.serverRunning(true);

        // Endless loop waiting for connections to come.
        while(true) {
            // Waiting for new connection.
            Socket newSocketConnection = serverSocket.accept();

            // Trying to create a player instance.
            // On success, added to "Players waiting" queue.
            new Player(newSocketConnection);
        }
    }
}
