package services;

import com.google.gson.JsonSyntaxException;
import dto.MessageType;
import dto.PlayerCommand;
import dto.ServerGeneralMessage;
import entities.player.HostEntity;
import exceptions.MatchTerminationException;
import exceptions.PlayerConnectionException;
import match.MatchMaking;
import model.player.PlayerEntity;
import utils.GlobalSettings;
import utils.json.JsonFormatter;
import utils.loggers.LoggerManager;
import utils.loggers.MatchLogger;
import utils.player.Location;

import java.net.SocketTimeoutException;
import java.util.List;

import static dto.PlayerAction.RIVAL_QUIT;

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
    public void run() {

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
    public void removeWaitingToQuitPlayers() throws Exception
    {

        if(this.m_WaitingToQuit.size() > 0)
        {
            this.m_MatchPlayerEntities.removeAll(this.m_WaitingToQuit);
            this.m_MatchQuitedMatchPlayerEntities.addAll(this.m_WaitingToQuit);

            this.m_WaitingToQuit.forEach((quitedPlayer) -> {

                this.SendPlayerCommand(new PlayerCommand(MessageType.COMMAND,
                        quitedPlayer.GetUserName(), RIVAL_QUIT, new Location(0,0)));

                MatchLogger.Debug(GetMatchIdentifier(), "Player " + quitedPlayer.GetUserName() + " disconnected.");
            });

            this.m_WaitingToQuit.clear();

            if (!this.m_IsGameOver
                    && this.m_MatchPlayerEntities.size() < GlobalSettings.MINIMUM_AMOUNT_OF_PLAYERS)
            {
                throw new MatchTerminationException(this.GetMatchIdentifier(), GlobalSettings.NOT_ENOUGH_PLAYERS_TO_CONTINUE);
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