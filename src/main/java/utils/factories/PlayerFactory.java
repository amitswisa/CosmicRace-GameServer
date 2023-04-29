package utils.factories;

import client.CouchPlayer;
import client.OnlinePlayer;
import client.Player;

import java.net.Socket;

public class PlayerFactory {
    public static Player createPlayer(Socket socket, boolean isOnlinePlayer){

        if(isOnlinePlayer){
            return new OnlinePlayer(socket);
        }else {
            return new CouchPlayer(socket);
        }
    }
}
