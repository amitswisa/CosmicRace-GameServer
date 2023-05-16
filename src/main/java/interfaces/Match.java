package interfaces;
import player.Player;

public interface Match {

    String GetMatchIdentifier();
    void EndMatch(String i_MatchEndedReason);
    boolean IsGameOver();
    void RemovePlayerFromMatch(Player player);
    void SendToAll(String message);

}