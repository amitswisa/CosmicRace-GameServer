package services;

import dto.PlayerCommand;
import dto.ServerGeneralMessage;
import entities.player.HostEntity;
import entities.player.WebPlayerEntity;
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
        this.waitForPlayersToBeReady();
        MatchLogger.Debug(GetMatchIdentifier(), "Players ready.");

        SendMessageToHost(new ServerGeneralMessage(ServerGeneralMessage.eActionType.DATA, getMatchPlayersAsJson()).toString());

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, "Starting match..").toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Start message sent.");
    }

    @Override
    public void run()
    {
        try {
            managePreGameStage();

            initMatch();

            setMatchStarted();


            //......
        } catch(Exception e) {
            LoggerManager.info("Room " + this.m_MatchIdentifier + ": " + e.getMessage());
        }
    }

    // while -> heartbeats for all players connected
    // WebPlayer -> when sends message it goes into queue.
    // read players queue and host buffer.

    private void managePreGameStage() throws Exception {
        boolean m_IsPreStageRunning = true;

        while(m_IsPreStageRunning)
        {
            try {
                String hostMessage = r_MatchHost.ReadMessage();

                if(hostMessage != null && hostMessage.equals("START\n"))
                {
                    m_IsPreStageRunning = false;
                }

            } catch (IOException e) {
                EndMatch(GlobalSettings.MATCH_TERMINATED);
            }

            actionOnMatchPlayers((playerEntity) -> {
                try {
                    String playerMessage = playerEntity.ReadMessage();

                    if(playerMessage != null)
                    {
                        LoggerManager.info("Player " + playerEntity.GetUserName() + ": " + playerMessage);
                    }

                } catch (IOException e) {
                    RemovePlayerFromMatch(playerEntity);
                }
            });

            removeWaitingToQuitPlayers();
        }
    }

    @Override
    protected void actionOnMatchPlayers(Consumer<PlayerEntity> processor)
    {
        boolean isHost = true;

        for (PlayerEntity matchEntity : m_MatchPlayerEntities)
        {
            if(isHost)
            {
                isHost = false;
                continue;
            }

            if (matchEntity.IsConnectionAlive())
            {
                try {
                    processor.accept(matchEntity);
                } catch (Exception e) {
                    LoggerManager.error("Player " + matchEntity.GetUserName() + " " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void EndMatch(String i_MatchEndedReason)
    {
        this.m_IsGameOver = true;
        LoggerManager.warning(i_MatchEndedReason);
    }

    synchronized private void SendMessageToHost(String i_Message)
    {
        try {
            this.r_MatchHost.SendMessage(i_Message);
            LoggerManager.info("Match Room (" + super.m_MatchIdentifier + "): " + i_Message);
        } catch(SocketTimeoutException ste) {
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
    public HostEntity GetHost(){
        return this.r_MatchHost;
    }
}
