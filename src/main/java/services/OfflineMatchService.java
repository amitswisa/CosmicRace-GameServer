package services;

import dto.PlayerCommand;
import dto.PlayerMatchInfo;
import dto.ServerGeneralMessage;
import entities.player.HostEntity;
import entities.player.WebPlayerEntity;
import match.OfflineMatchManager;
import model.player.PlayerEntity;
import utils.GlobalSettings;
import utils.json.JsonFormatter;
import utils.loggers.LoggerManager;
import utils.loggers.MatchLogger;
import utils.match.MatchScoreManager;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.function.Consumer;

public class OfflineMatchService extends MatchService
{
    private final HostEntity r_MatchHost;

    public OfflineMatchService(HostEntity i_HostEntity, List<PlayerEntity> i_MatchPlayersList, String i_MatchIdentifier)
    {
        super(i_MatchIdentifier, i_MatchPlayersList);
        this.r_MatchHost = i_HostEntity;
        SendMessageToHost(new ServerGeneralMessage(ServerGeneralMessage.eActionType.ROOM_CREATED, i_MatchIdentifier).toString());
    }

    @Override
    protected void initMatch() throws Exception
    {
        SendMessageToHost(new ServerGeneralMessage(ServerGeneralMessage.eActionType.DATA, getMatchPlayersAsJson()).toString());

        SetAllPlayerAlive();

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, "Starting match..").toString());

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.ACTION, "START").toString());

        this.SendMessageToHost(new ServerGeneralMessage(ServerGeneralMessage.eActionType.ACTION, "START").toString());
        MatchLogger.Info(GetMatchIdentifier(), "Starting game.");

        setMatchStarted();
    }

    @Override
    public void run()
    {
        try {
            managePreGameStage();

            initMatch();

            runGame();

        } catch(Exception e) {
            LoggerManager.info("Room " + this.m_MatchIdentifier + ": " + e.getMessage());
            EndMatch(e.getMessage());
        }
    }

    private void managePreGameStage() throws Exception
    {
        boolean m_IsPreStageRunning = true;

        while(m_IsPreStageRunning)
        {
            String hostMessage = r_MatchHost.ReadMessage();

            if(hostMessage != null)
            {
                if(hostMessage.equals("START"))
                {
                    m_IsPreStageRunning = false;
                    continue;
                } else if(hostMessage.equals("QUIT")) {
                    this.EndMatch("Host closed match room.");
                }
            }

            removeWaitingToQuitPlayers();
        }
    }

    @Override
    protected void actionOnMatchPlayers(Consumer<PlayerEntity> processor)
    {
        for (PlayerEntity matchEntity : m_MatchPlayerEntities)
        {
            if(matchEntity instanceof HostEntity)
                continue;

            if (matchEntity.IsConnectionAlive())
            {
                try {
                    processor.accept(matchEntity);
                } catch (Exception e) {
                    LoggerManager.error("Player " + matchEntity.GetUserName() + " " + e.getMessage());
                    RemovePlayerFromMatch(matchEntity);
                }
            }
        }
    }

    @Override
    public void EndMatch(String i_MatchEndedReason) {

        if(this.m_IsGameOver) return;

        this.m_IsGameOver = true;
        MatchLogger.Info(GetMatchIdentifier(), "Terminating match...");

        if(i_MatchEndedReason == null)
        {
            i_MatchEndedReason = GlobalSettings.NOT_ENOUGH_PLAYERS_TO_CONTINUE;
        }
        else if(!i_MatchEndedReason.equals(GlobalSettings.MATCH_ENDED))
        {
            MatchLogger.Error(GetMatchIdentifier(), i_MatchEndedReason);
        }
        else
        {
            MatchLogger.Info(GetMatchIdentifier(), i_MatchEndedReason);
        }

        //Ran
        if(this.GetNumOfActivePlayers() == 1) //if there's only 1 player left - EXCLUDING HOST
        {
            PlayerEntity lastPlayer = this.m_MatchPlayerEntities.get(1); //this.m_MatchPlayerEntities.get(0) is THE HOST.

            if(lastPlayer.IsConnectionAlive() && !lastPlayer.IsFinishedMatch())
                this.m_MatchScore.SetPlayerScore(lastPlayer);
        }

        this.m_MatchScore.UnionScoreBoards(); // Union disconnected stack and players finished list.
        List<MatchScoreManager.PlayerScore> finalScoreTable = this.m_MatchScore.GetPlayersMatchScoreList();

        ServerGeneralMessage finalMatchEndedMessage
                = new ServerGeneralMessage(ServerGeneralMessage.eActionType.COMPLETE_MATCH, i_MatchEndedReason);



        try
        {
            for (MatchScoreManager.PlayerScore player : finalScoreTable)
            {
                ServerGeneralMessage scorePositionAnnouncement
                        = new ServerGeneralMessage
                        (ServerGeneralMessage.eActionType.COMPLETE_LEVEL,
                                (new PlayerMatchInfo(player.GetPlayer().GetUserName(), this.m_MatchScore.GetFinalLocation(player.GetPlayer().GetUserName()), player.GetPlayer().GetCollectedCoinsAmount()).toString()));

                SendMessageToHost(scorePositionAnnouncement.toString());
            }

            this.r_MatchHost.SendMessage(finalMatchEndedMessage.toString());

        } catch (SocketTimeoutException ste) {
            MatchLogger.Info(this.m_MatchIdentifier,"Couldn't notify host on match ending.");
        } finally {
            this.r_MatchHost.CloseConnection(i_MatchEndedReason);
        }

        String finalI_MatchEndedReason = i_MatchEndedReason;
        this.actionOnMatchPlayers((player) -> {
            try {
                player.SendMessage(finalMatchEndedMessage.toString());
            } catch (SocketTimeoutException e) {
                MatchLogger.Warning(GetMatchIdentifier()
                        , "Couldn't update player " + player.GetUserName() + " on match ending.");
            } finally {
                player.CloseConnection(finalI_MatchEndedReason);
            }
        });

        OfflineMatchManager.RemoveActiveMatch(this.m_MatchIdentifier);
    }

    synchronized private void SendMessageToHost(String i_Message)
    {
        try {
            this.r_MatchHost.SendMessage(i_Message);
            LoggerManager.info("Match Room (" + super.m_MatchIdentifier + "): " + i_Message);
        } catch(SocketTimeoutException ste) {
            LoggerManager.info("Match Room (" + super.m_MatchIdentifier + "): Couldn't notify host - ending match.");
            EndMatch(ste.getMessage());
        }
    }

    @Override
    public void SendPlayerCommand(PlayerCommand i_PlayerCommand)
    {
        String command = JsonFormatter.GetGson().toJson(i_PlayerCommand, PlayerCommand.class);
        SendMessageToHost(command);
    }

    synchronized public void AddPlayer(WebPlayerEntity i_NewWebPlayer)
    {
        this.m_MatchPlayerEntities.add(i_NewWebPlayer);
        i_NewWebPlayer.SetMatch(this);

        SendMessageToHost(new ServerGeneralMessage
                (ServerGeneralMessage.eActionType.PLAYER_JOINED, i_NewWebPlayer.GetUserName()).toString());

        LoggerManager.info(i_NewWebPlayer.GetUserName() + " has connected to room " + this.m_MatchIdentifier);
    }

    @Override
    public HostEntity GetHost()
    {
        return this.r_MatchHost;
    }

    @Override
    public int GetNumOfActivePlayers()
    {
        return this.m_MatchPlayerEntities.size() - 1;
    }

    synchronized public boolean IsGameStarted()
    {
        return this.m_IsGameStarted;
    }
}
