package dto;

public class PlayerSummary {
    private String m_PlayerName;
    private int m_Location;
    private int m_CoinsCollected;

    public PlayerSummary(String m_PlayerName, int m_Location, int m_CoinsCollected) {
        this.m_PlayerName = m_PlayerName;
        this.m_Location = m_Location;
        this.m_CoinsCollected = m_CoinsCollected;
    }
}
