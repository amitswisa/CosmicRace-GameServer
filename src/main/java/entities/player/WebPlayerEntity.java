package entities.player;

import com.google.gson.JsonObject;
import dto.MessageType;
import model.connection.ConnectionModel;
import model.player.PlayerEntity;
import utils.json.JsonFormatter;
import utils.loggers.LoggerManager;
import utils.player.Character;

import java.net.SocketTimeoutException;

public class WebPlayerEntity extends PlayerEntity
{

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
        JsonObject playerData = JsonFormatter.createJsonFromString(i_Text);
        String messageType = playerData.get("messagetype").getAsString();

        if(messageType.equals(MessageType.CONFIGURATION))
        {
            int characterId = playerData.get("characterid").getAsInt();
            m_Username = playerData.get("username").getAsString().replace("\"", "");
            m_Character = new Character(characterId, "None", 1, 0, 10, 50, 10, 3, 1, 0, 0);
            this.MarkAsReady();

            LoggerManager.info(m_Username + " is Ready.");
        }
        else
        {
            m_Connection.AddMessageToQueue(i_Text);
        }

        // Your logic for handling other messages
        System.out.println("Received message: " + i_Text);
    }



    public void sendMessageToHost(String i_Message){ //RAN

        try {
            HostEntity hostEntity = (HostEntity)this.m_CurrentMatch.GetHost();
            hostEntity.SendMessage(i_Message);
        } catch (SocketTimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
