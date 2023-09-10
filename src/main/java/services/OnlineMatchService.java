package services;

import dto.PlayerCommand;
import dto.PlayerMatchInfo;
import dto.ServerGeneralMessage;
import entities.player.HostEntity;
import match.MatchMaking;
import model.player.PlayerEntity;
import utils.GlobalSettings;
import utils.json.JsonFormatter;
import utils.loggers.LoggerManager;
import utils.loggers.MatchLogger;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.function.Consumer;

public final class OnlineMatchService extends MatchService {

    public OnlineMatchService(String i_MatchIdentifier, List<PlayerEntity> i_MatchPlayersList)
    {
        super(i_MatchIdentifier, i_MatchPlayersList);

        this.actionOnMatchPlayers(player -> player.SetMatch(this));

        SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, "Players found, creating a match.").toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Trying to create a match!");

        SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.DATA, getMatchPlayersAsJson()).toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Players initial data sent!");
    }

    @Override
    protected void initMatch() throws Exception
    {

        this.waitForPlayersToBeReady();
        MatchLogger.Debug(GetMatchIdentifier(), "Players ready.");

        SetAllPlayerAlive();

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, "Starting match..").toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Start message sent.");

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.ACTION, "START").toString());
        MatchLogger.Info(GetMatchIdentifier(), "Starting game.");
    }

    @Override
    public void run() {
        setMatchStarted();

        try
        {
            this.initMatch();

            this.runGame();

        } catch(Exception e){
            this.EndMatch(e.getMessage());
        }
    }

    @Override
    protected void actionOnMatchPlayers(Consumer<PlayerEntity> processor)
    {
        for (PlayerEntity matchEntity : m_MatchPlayerEntities) {
            if (matchEntity.IsConnectionAlive()) {
                try {
                    processor.accept(matchEntity);
                } catch (Exception e) {
                    LoggerManager.error("Player " + matchEntity.GetUserName() + " " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void SendPlayerCommand(PlayerCommand i_PlayerCommand)
    {
        actionOnMatchPlayers((player) -> {

            if(!player.GetUserName().equals(i_PlayerCommand.GetUsername()))
            {
                try {
                    String command = JsonFormatter.GetGson().toJson(i_PlayerCommand, PlayerCommand.class);

                    player.SendMessage(command);
                } catch(SocketTimeoutException ste) {
                    player.CloseConnection(ste.getMessage());
                }
            }
        });

        LoggerManager.trace(i_PlayerCommand.toString());
    }

    @Override
    public void EndMatch(String i_MatchEndedReason) {

        if(this.m_IsGameOver) return;

        this.m_IsGameOver = true;
        ServerGeneralMessage finalMatchEndedMessage;

        if(!i_MatchEndedReason.equals(GlobalSettings.MATCH_ENDED))
        {
            MatchLogger.Error(GetMatchIdentifier(), i_MatchEndedReason);
            finalMatchEndedMessage
                    = new ServerGeneralMessage(ServerGeneralMessage.eActionType.MATCH_TERMINATION, i_MatchEndedReason);
        }
        else
        {
            finalMatchEndedMessage
                    = new ServerGeneralMessage(ServerGeneralMessage.eActionType.COMPLETE_MATCH, i_MatchEndedReason);
            MatchLogger.Info(GetMatchIdentifier(), i_MatchEndedReason);
            UpdateGameStatistics();
        }

        for (PlayerEntity player : m_MatchPlayerEntities) {
            ServerGeneralMessage scorePositionAnnouncement
                    = new ServerGeneralMessage
                    (ServerGeneralMessage.eActionType.COMPLETE_LEVEL,
                            (new PlayerMatchInfo(player.GetUserName(), this.m_MatchScore.GetFinalLocation(player.GetUserName()), player.GetCollectedCoinsAmount()).toString()));

            SendMessageToAll(scorePositionAnnouncement.toString());
        }

        for (PlayerEntity player : m_MatchPlayerEntities) {
            try {
                player.SendMessage(finalMatchEndedMessage.toString());
                MatchLogger.Info(this.m_MatchIdentifier, "End match message to " + player.GetUserName() + " " + finalMatchEndedMessage.toString());
            } catch (SocketTimeoutException e) {
                MatchLogger.Warning(GetMatchIdentifier()
                        , "Couldn't update player " + player.GetUserName() + " on match ending.");
            } finally {
                player.CloseConnection(i_MatchEndedReason);
            }
        }

        MatchMaking.RemoveActiveMatch(this);
        this.interrupt();
    }

    @Override //RAN
    public HostEntity GetHost(){
        return (HostEntity) this.m_MatchPlayerEntities.get(0);
    }

    @Override
    public int GetNumOfActivePlayers()
    {
        return this.m_MatchPlayerEntities.size();
    }
}