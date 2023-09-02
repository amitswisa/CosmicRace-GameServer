package entities.player;

import model.player.PlayerEntity;
import model.connection.ConnectionModel;

public final class HostEntity extends PlayerEntity
{

    public HostEntity(ConnectionModel i_Connection)
    {
        super(i_Connection);
    }

    @Override
    public void CloseConnection(String i_ExceptionMessage) {
        return; //TODO
    }

}
