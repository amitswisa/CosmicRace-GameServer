package entities.player;

import model.player.PlayerEntity;
import model.connection.ConnectionModel;

public class WebPlayerEntity extends PlayerEntity {

    public WebPlayerEntity(ConnectionModel i_Connection)
    {
        super(i_Connection);
    }

    @Override
    public void CloseConnection(String i_ExceptionMessage)
    {
        this.m_Connection.CloseConnection(i_ExceptionMessage);
    }

    public void HandleMessageReceived(String i_Text)
    {
        // Your logic for handling other messages
        System.out.println("Received message: " + i_Text);
    }
}
