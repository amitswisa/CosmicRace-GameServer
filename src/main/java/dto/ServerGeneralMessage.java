package dto;

import com.google.gson.JsonObject;
import utils.json.JsonFormatter;

public final class ServerGeneralMessage {

    public enum eActionType {
        NOTIFICATION,
        DATA,
        ACTION,
        ERROR,
        CONFIRMATION,
        MATCH_TERMINATION,
        COMPLETE_MATCH,
        MATCH_ENDED,
        ROOM_CREATED,
        PLAYER_JOINED,
        PLAYER_READY
    }

    private eActionType m_ActionType;
    private String m_MessageContent;

    public ServerGeneralMessage(eActionType i_ActionType, String i_MessageContent) {
        this.m_ActionType = i_ActionType;
        this.m_MessageContent = i_MessageContent;
    }

    @Override
    public String toString() {
        JsonObject res = new JsonObject();
        res.addProperty("m_MessageType", "MESSAGE");
        res.addProperty("ActionType", this.m_ActionType.toString());
        res.addProperty("MessageContent", this.m_MessageContent);
        return JsonFormatter.GetGson().toJson(res);
    }
}
