package dto;

import com.google.gson.JsonObject;

public class ClientMessage {

    enum MessageType {
        NOTIFICATION, DATA, ACTION, ERROR
    }

    private MessageType messageType;
    private String content;

    public ClientMessage() {

    }

    public JsonObject getMessage() {
        return null;
    }

}
