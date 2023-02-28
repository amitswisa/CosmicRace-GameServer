package dto;

public class SocketMessage {

    public enum MessageType {
        JOIN_GAME,
        MOVE_PLAYER,
        PLAYER_JUMP,
        PLAYER_DUCK,
        PLAYER_POWER1,
        PLAYER_POWER2,
        PLAYER_POWER3,
        QUIT_GAME,
    }

    private MessageType messageType;
    private int length;
    private String message;

    public SocketMessage(MessageType messageType, String message) {
        this.messageType = messageType;
        this.message = message;
        this.length = this.message.length();
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public int getLength() {
        return length;
    }

    public String getMessage() {
        return message;
    }
}
