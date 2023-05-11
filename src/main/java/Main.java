import player.Player;
import match_making.MatchMaking;
import utils.Utils;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws Exception {

        ServerSocket serverSocket = new ServerSocket(Utils.PORT);

        while(true) {
            Socket newSocketConnection = serverSocket.accept();
            MatchMaking.addPlayerToWaitingList(new Player(newSocketConnection));
        }
    }
}