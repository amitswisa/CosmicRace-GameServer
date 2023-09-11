package entities.player;

import dto.ServerGeneralMessage;
import match.MatchMaking;
import model.player.PlayerEntity;
import model.connection.ConnectionModel;
import utils.loggers.LoggerManager;

import java.net.SocketTimeoutException;

public final class HostEntity extends PlayerEntity
{

    public HostEntity(ConnectionModel i_Connection)
    {
        super(i_Connection);
    }

    @Override
    public void CloseConnection(String i_ExceptionMessage)
    {
        if(this.m_CurrentMatch != null)
            this.m_CurrentMatch.RemovePlayerFromMatch(this);

        // Notify player on connection close.
        try {
            this.SendMessage(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, i_ExceptionMessage).toString());
        } catch(SocketTimeoutException ste) {
            LoggerManager.warning("Couldn't notify player " + this.m_Username + " on " + i_ExceptionMessage);
        }

        this.m_Connection.CloseConnection(i_ExceptionMessage);
    }

}
