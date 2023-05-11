package player;

import java.net.Socket;
import java.net.SocketException;

public class OnlinePlayer extends Player
{

    public OnlinePlayer(Socket socketConnection) throws SocketException {
        super(socketConnection);
    }

}
