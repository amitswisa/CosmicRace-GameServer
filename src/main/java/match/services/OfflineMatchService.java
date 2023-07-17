package match.services;

import dto.ServerGeneralMessage;
import match.entities.match_host.MatchHostEntity;
import utils.loggers.LoggerManager;

import java.net.SocketTimeoutException;

public class OfflineMatchService extends Thread
{
    private final String r_MatchIdentifier;
    private final MatchHostEntity r_MatchHost;
    private boolean m_IsGameOver;

    public OfflineMatchService(MatchHostEntity i_MatchHost, String i_MatchIdentifier)
    {
        this.m_IsGameOver = false;
        this.r_MatchIdentifier = i_MatchIdentifier;
        this.r_MatchHost = i_MatchHost;

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
}
