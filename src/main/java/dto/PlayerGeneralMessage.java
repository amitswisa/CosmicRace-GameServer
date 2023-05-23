package dto;

import com.google.gson.JsonObject;
import utils.json.JsonFormatter;

public final class PlayerGeneralMessage {

    public enum MessageType {
        NOTIFICATION,
        DATA,
        ACTION,
        ERROR,
        CONFIRMATION
    }

    private MessageType m_MessageType;
    private String m_MessageContent;

    public PlayerGeneralMessage(MessageType i_MessageType, String i_MessageContent) {
        this.m_MessageType = i_MessageType;
        this.m_MessageContent = i_MessageContent;
    }

    @Override
    public String toString() {
        JsonObject res = new JsonObject();
        res.addProperty("m_MessageType", "MESSAGE");
        res.addProperty("ActionType", this.m_MessageType.toString());
        res.addProperty("MessageContent", this.m_MessageContent);
        return JsonFormatter.GetGson().toJson(res);
    }
}
