package services;

import dto.PlayerCommand;
import dto.ServerGeneralMessage;
import entities.player.HostEntity;
import entities.player.WebPlayerEntity;
import match.MatchMaking;
import match.OfflineMatchManager;
import model.player.PlayerEntity;
import utils.GlobalSettings;
import utils.json.JsonFormatter;
import utils.loggers.LoggerManager;
import utils.loggers.MatchLogger;

import java.io.IOException;
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

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, "Starting match..").toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Start message sent.");

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

            if(hostMessage != null && hostMessage.equals("START"))
            {
                m_IsPreStageRunning = false;
                continue;
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
            {
                continue;
            }

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

        this.m_IsGameOver = true;

        if(!i_MatchEndedReason.equals(GlobalSettings.MATCH_ENDED))
        {
            MatchLogger.Error(GetMatchIdentifier(), i_MatchEndedReason);
        }
        else
        {
            MatchLogger.Info(GetMatchIdentifier(), i_MatchEndedReason);
        }

        // TODO - update players coins and stats on database.
        //  /30.4/UPDATE - only stats left.
        //this.actionOnMatchPlayers(p -> DBHandler.updateStatsInDB(p.GetCharacter()));

        ServerGeneralMessage finalMatchEndedMessage
                = new ServerGeneralMessage(ServerGeneralMessage.eActionType.MATCH_TERMINATION, i_MatchEndedReason);

        // Notify host.
        try
        {
            this.r_MatchHost.SendMessage(finalMatchEndedMessage.toString());
        } catch (SocketTimeoutException ste) {
            MatchLogger.Info(this.m_MatchIdentifier,"Couldn't update host on match ending.");
        }

        this.actionOnMatchPlayers((player) -> {
            try {
                player.SendMessage(finalMatchEndedMessage.toString());
            } catch (SocketTimeoutException e) {
                MatchLogger.Warning(GetMatchIdentifier()
                        , "Couldn't update player " + player.GetUserName() + " on match ending.");
            }
        });

        OfflineMatchManager.RemoveActiveMatch(this.m_MatchIdentifier);
        this.interrupt();
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
}
