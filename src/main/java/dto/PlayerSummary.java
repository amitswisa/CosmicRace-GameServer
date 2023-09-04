package dto;

public class PlayerSummary {
    private String playerName;
    private int position;
    private int coinsCollected;

    public PlayerSummary(String playerName, int position, int coinsCollected) {
        this.playerName = playerName;
        this.position = position;
        this.coinsCollected = coinsCollected;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getPosition() {
        return position;
    }

    public int getCoinsCollected() {
        return coinsCollected;
    }
}
