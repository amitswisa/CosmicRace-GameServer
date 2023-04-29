package interfaces;
import client.Player;

public interface Match {

    public String getIdentifyer();
    public void endMatch();
    public boolean isGameOver();
    public void removePlayerFromMatch(Player player);
    public void broadCastToAll(String message);


}
