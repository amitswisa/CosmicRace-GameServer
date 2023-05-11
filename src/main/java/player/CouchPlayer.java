package player;

import java.net.Socket;
import java.net.SocketException;

public class CouchPlayer extends Player {
    public CouchPlayer(Socket socketConnection) throws SocketException {
        super(socketConnection);
    }


}
