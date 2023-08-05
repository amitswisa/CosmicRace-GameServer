package utils.match;

import model.player.PlayerEntity;
import utils.GlobalSettings;

import java.util.ArrayList;
import java.util.List;

public final class MatchScoreManager {

    private class PlayerScore
    {
        private final PlayerEntity m_Match_PlayerEntity;
        private final int m_MatchScorePosition;

        public PlayerScore(PlayerEntity i_Match_PlayerEntity, int i_MatchScorePosition)
        {
            this.m_Match_PlayerEntity = i_Match_PlayerEntity;
            this.m_MatchScorePosition = i_MatchScorePosition;
        }

        public int GetScorePosition()
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

    public int SetPlayerScore(PlayerEntity i_Match_PlayerEntity) throws  IllegalArgumentException
    {
        if(m_ScoreBoard.contains(i_Match_PlayerEntity))
            throw new IllegalArgumentException("Player already exists in score list");

        PlayerScore playerScore = new PlayerScore(i_Match_PlayerEntity, m_BoardPosition);
        this.m_ScoreBoard.add(playerScore);

        this.m_BoardPosition += 1;

        return playerScore.GetScorePosition();
    }

    public int GetFinalLocation(String i_UserName){
        for (PlayerScore player : m_ScoreBoard) {
            if(player.m_Match_PlayerEntity.GetUserName().equals(i_UserName)){
                return player.GetScorePosition();
            }
        }
        return -1;
    }
}
