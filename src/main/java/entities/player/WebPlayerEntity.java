package entities.player;

import com.google.gson.JsonObject;
import dto.ServerGeneralMessage;
import model.connection.ConnectionModel;
import model.player.PlayerEntity;
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
        if(this.m_CurrentMatch != null)
            this.m_CurrentMatch.RemovePlayerFromMatch(this);

        // Notify player on connection close.
        if(this.m_CurrentMatch != null
                && this.m_CurrentMatch.IsGameOver()) {
            try {
                this.SendMessage(new ServerGeneralMessage(ServerGeneralMessage.eActionType.NOTIFICATION, i_ExceptionMessage).toString());
            } catch (SocketTimeoutException ste) {
                LoggerManager.warning("Couldn't notify player " + this.m_Username + " on " + i_ExceptionMessage);
            }
        }

        this.m_Connection.CloseConnection(i_ExceptionMessage);

    }

    public void HandleMessageReceived(String i_Text)
    {
        m_Connection.AddMessageToQueue(i_Text);
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
