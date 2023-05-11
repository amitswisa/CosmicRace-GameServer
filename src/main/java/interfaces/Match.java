package interfaces;
import player.Player;

public interface Match {

    String GetMatchIdentifier();
    void EndMatch();
    boolean IsGameOver();
    void RemovePlayerFromMatch(Player player);
    void SendToAll(String message);

}