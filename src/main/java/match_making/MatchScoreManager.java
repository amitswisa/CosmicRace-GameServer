package match_making;

import dto.ServerGeneralMessage;
import player.Player;
import utils.GlobalSettings;

import java.util.ArrayList;
import java.util.List;

public final class MatchScoreManager {

    private class PlayerScore
    {
        private final Player m_Player;
        private final int m_MatchScorePosition;

        public PlayerScore(Player i_Player, int i_MatchScorePosition)
        {
            this.m_Player = i_Player;
            this.m_MatchScorePosition = i_MatchScorePosition;
        }

        public int GetScorePositoin()
        {
            return this.m_MatchScorePosition;
        }
    }

    private final List<PlayerScore> m_ScoreBoard;
    private int m_BoardPosition;

    public MatchScoreManager()
    {
        m_ScoreBoard = new ArrayList<>(GlobalSettings.MAXIMUM_AMOUNT_OF_PLAYERS);
        m_BoardPosition = 1;
    }

    public int SetPlayerScore(Player i_Player) throws  IllegalArgumentException
    {
        if(m_ScoreBoard.contains(i_Player))
            throw new IllegalArgumentException("Player already exists in score list");

        PlayerScore playerScore = new PlayerScore(i_Player, m_BoardPosition);
        this.m_ScoreBoard.add(playerScore);

        this.m_BoardPosition += 1;

        return playerScore.GetScorePositoin();
    }
}
