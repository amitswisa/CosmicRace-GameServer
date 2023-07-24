package services;

import dto.PlayerCommand;
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

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, "Starting match..").toString());
        MatchLogger.Debug(GetMatchIdentifier(), "Start message sent.");

        this.SendMessageToAll(new ServerGeneralMessage(ServerGeneralMessage.eActionType.ACTION, "START").toString());
        MatchLogger.Info(GetMatchIdentifier(), "Starting game.");
    }

    @Override
    public void run() {
        setMatchStarted();
        PlayerCommand playerCommand = new PlayerCommand();

        try
        {
            this.initMatch();

            while (!isMatchOver())
            {
                for(PlayerEntity matchPlayer : m_MatchPlayerEntities)
                {
                    try {
                        String playerUpdate = matchPlayer.ReadMessage();

                        if(!playerUpdate.equals(GlobalSettings.NO_MESSAGES_IN_CLIENT_BUFFER)) {
                            LoggerManager.info(playerUpdate);
                            playerCommand.ParseFromJson(playerUpdate);
                            this.handlePlayerResponse(matchPlayer, playerCommand);
                        }
                    }
                    catch(PlayerConnectionException pqe)
                    {
                        matchPlayer.CloseConnection(pqe.getMessage());
                    }
                    catch(JsonSyntaxException jse)
                    {
                        MatchLogger.Error(this.GetMatchIdentifier()
                                , "Player " + matchPlayer.GetUserName() + " command error: " + jse.getMessage());
                    }
                }

                this.removeWaitingToQuitPlayers();
            }

            this.EndMatch(GlobalSettings.MATCH_ENDED);

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

        this.actionOnMatchPlayers((player) -> {
            try {
                player.SendMessage(finalMatchEndedMessage.toString());
            } catch (SocketTimeoutException e) {
                MatchLogger.Warning(GetMatchIdentifier()
                        , "Couldn't update player " + player.GetUserName() + " on match ending.");
            }
        });

        MatchMaking.RemoveActiveMatch(this);
        this.interrupt();
    }

    @Override //RAN
    public HostEntity GetHost(){
        return null;
    }
}