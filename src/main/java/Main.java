import factories.PlayerFactory;
import utils.GlobalSettings;

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