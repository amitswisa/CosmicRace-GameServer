package client;

import utils.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;


public class User {

    public static void main(String[] args) {
        try {
            Socket clientSocket = new Socket(Utils.HOST, Utils.PORT);

            System.out.println("Client " + clientSocket + " is listening on port " + clientSocket.getPort());

            OutputStream outputStream = clientSocket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream, true);

            printWriter.println(Utils.AUTH_KEY);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
