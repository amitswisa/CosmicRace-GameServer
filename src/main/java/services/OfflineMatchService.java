package services;

import dto.PlayerCommand;
import dto.ServerGeneralMessage;
import entities.player.MatchHostEntity;
import model.player.MatchPlayerEntity;
import utils.json.JsonFormatter;
import utils.loggers.LoggerManager;

import java.net.SocketTimeoutException;
import java.util.List;

public class OfflineMatchService extends Thread
{
    private final String r_MatchIdentifier;
    private final MatchHostEntity r_MatchHost;
    private boolean m_IsGameOver;

    public OfflineMatchService(MatchHostEntity i_HostEntity, List<MatchPlayerEntity> i_MatchPlayersList, String i_MatchIdentifier)
    {
        super(i_MatchIdentifier, i_MatchPlayersList);
        this.m_IsGameOver = false;
        this.r_MatchIdentifier = i_MatchIdentifier;
        this.r_MatchHost = i_HostEntity;

        SendMessageToHost(new ServerGeneralMessage(ServerGeneralMessage.eActionType.ROOM_CREATED, i_MatchIdentifier).toString());
    }

    @Override
    public void run()
    {

    }

    public boolean IsGameOver()
    {
        return this.m_IsGameOver;
    }

    private void SendMessageToHost(String i_Message)
    {
        try {
            this.r_MatchHost.SendMessage(i_Message);
            LoggerManager.info("Match Room (" + r_MatchIdentifier + "): Room created!");
        } catch(SocketTimeoutException ste) {
            TerminateMatch(ste.getMessage());
        }
    }

    private void TerminateMatch(String message)
    {
        this.m_IsGameOver = true;
        LoggerManager.warning(message);
    }

    public void SendPlayerCommand(PlayerCommand i_PlayerCommand)
    {
        actionOnMatchPlayers((player) -> {

            if(!player.GetUserName().equals(i_PlayerCommand.GetUsername()))
            {
                try {
                    String command = JsonFormatter.GetGson().toJson(i_PlayerCommand, PlayerCommand.class);

                    m_MatchPlayerEntities.get(0).SendMessage(command);
                } catch(SocketTimeoutException ste) {
                    player.CloseConnection(ste.getMessage());
                }
            }
        });

        LoggerManager.trace(i_PlayerCommand.toString());
    }
}
