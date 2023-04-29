package client;

import java.net.Socket;
public class OnlinePlayer extends Player{
    public OnlinePlayer(Socket socketConnection) {
        super(socketConnection);
    }
}
