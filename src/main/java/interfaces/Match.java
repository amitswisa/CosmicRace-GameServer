package interfaces;
import player.Player;

public interface Match {

    String getM_MatchIdentifier();
    void endMatch();
    boolean isM_IsGameOver();
    void removePlayerFromMatch(Player player);
    void sendToAll(String message);

}