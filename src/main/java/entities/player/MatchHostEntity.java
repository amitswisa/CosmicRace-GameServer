package entities.player;

import model.player.MatchPlayerEntity;
import model.connection.Connection;

public final class MatchHostEntity extends MatchPlayerEntity
{

    public MatchHostEntity(Connection i_Connection)
    {
        super(i_Connection);
    }

    @Override
    public void CloseConnection(String i_ExceptionMessage) {
        return;
    }

}
