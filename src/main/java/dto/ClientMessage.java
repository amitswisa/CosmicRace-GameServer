package dto;

import com.google.gson.JsonObject;
import json.JsonFormatter;
import utils.Utils;

public final class ClientMessage {

    public enum MessageType {
        NOTIFICATION, DATA, ACTION, ERROR, CONFIRMATION, GAME_OPERATION
    }

    private MessageType messageType;
    private String message;

    public ClientMessage(MessageType messageType, String content) {
        this.messageType = messageType;
        this.message = content;
    }

    @Override
    public String toString() {
        JsonObject res = new JsonObject();
        res.addProperty("messageType", this.messageType.toString());
        res.addProperty("message", this.message);
        return JsonFormatter.GetGson().toJson(res);
    }
}
