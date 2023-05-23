package exceptions;

public class PlayerQuitException extends Exception {

    public PlayerQuitException(String i_ExceptionMessage) {
        super(i_ExceptionMessage);
    }
}
