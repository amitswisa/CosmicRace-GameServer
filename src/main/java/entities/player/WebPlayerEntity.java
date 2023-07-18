package entities.player;

import com.google.gson.JsonObject;
import dto.MessageType;
import dto.ServerGeneralMessage;
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
        m_Connection.AddMessageToQueue(i_Text);
        System.out.println("Received message: " + i_Text);
    }

    public void sendMessageToHost(String i_Message)
    {
        try {
            PlayerEntity hostEntity = this.m_CurrentMatch.GetHost();
            hostEntity.SendMessage(i_Message);
        } catch (SocketTimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void SetInitData(JsonObject i_PlayerData)
    {
        int characterId = i_PlayerData.get("characterid").getAsInt();
        m_Username = i_PlayerData.get("username").getAsString().replace("\"", "");
        m_Character = new Character(characterId, "None", 1, 0, 10, 50, 10, 3, 1, 0, 0);
    }
}
