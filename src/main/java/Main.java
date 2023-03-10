import utils.MatchMaking;
import utils.Utils;
import client.Player;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws Exception {

        // Create server socket listening on port 6666;
        ServerSocket serverSocket = new ServerSocket(Utils.PORT);

        // Endless loop waiting for connections to come.
        while(true) {
            // Waiting for new connection.
            Socket newSocketConnection = serverSocket.accept();

            // Create thread to handle new socket.
            MatchMaking.addPlayerToWaitingList(new Player(newSocketConnection)); // Add socket to socket's list.
        }
    }
}
