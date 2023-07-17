package entities.player;

import model.player.MatchPlayerEntity;
import model.connection.Connection;

public class MatchWebPlayerEntity extends MatchPlayerEntity {

    public MatchWebPlayerEntity(Connection i_Connection) {

        super(i_Connection);
    }

    @Override
    public void CloseConnection(String i_ExceptionMessage) {

    }

    public void HandleMessageReceived(String i_Text) {
        handleOtherMessages(i_Text);
    }

    private void handleOtherMessages(String i_Text) {
        // Your logic for handling other messages
        System.out.println("Received message: " + i_Text);
    }

}
