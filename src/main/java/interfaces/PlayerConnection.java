package interfaces;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketTimeoutException;

public interface PlayerConnection {

    void SendMessage(String i_Message) throws SocketTimeoutException;

    void CloseConnection(String i_ExceptionMessage);

    boolean IsConnectionAlive();

    String ReadMessage() throws IOException;

    String WaitForPlayerResponse() throws IOException;

    String getHost();

    BufferedReader GetInStream();

}
