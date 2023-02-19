package client;
import utils.Utils;
import utils.LoggerManager;
import utils.SocketManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GameSession extends Thread {

    private int SESSION_ID; // Session ID.
    private final Socket socketConnection; // Client's socket.
    private PrintWriter out_stream; // Output stream.
    private BufferedReader in_stream; // Input stream.

    // GameSession constructor.
    public GameSession(Socket socketConnection) {

        this.socketConnection = socketConnection;
        try {
            // Get client's I/O tunnels.
            this.out_stream = new PrintWriter(this.socketConnection.getOutputStream(), true);
            this.in_stream = new BufferedReader(new InputStreamReader(this.socketConnection.getInputStream()));

            // Authenticate user connection.
            if(!this.in_stream.readLine().equals(Utils.AUTH_KEY)) {
                this.out_stream.println(Utils.UN_AUTHORIZED);
                throw new Exception(Utils.UN_AUTHORIZED);
            }

            SocketManager.add(this); // Add socket to socket's list.

        } catch(Exception e) {
            LoggerManager.error("Error occurred with " + this.getHost() + ": " + e.getMessage());
            closeConnection();
        }
    }

    public void run() {

        try {
            String line;
            while ((line = this.in_stream.readLine()) != null) {

                // Handle client request to play online.

            }

        } catch (IOException e) {
            SocketManager.remove(this);
            LoggerManager.debug("Error handling client ("+ this.getHost() +"): " + e.getMessage());
        } finally {
            closeConnection();
        }

    }

    // Send message to customer.
    public void send(String message) {
        try {
            out_stream.println(message);
        } catch(Exception e) {
            LoggerManager.error(e.getMessage());
        }
    }

    // Close socket connection.
    public void closeConnection() {
        try {
            LoggerManager.info("Socket (" + this.getHost() + " #" + this.SESSION_ID + "): Connection closed!");
            SocketManager.remove(this);
            this.socketConnection.close();
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // Get host address as known as IP address.
    public String getHost() {
        return this.socketConnection.getInetAddress().getHostAddress();
    }

    // Set current session unique id.
    public void setSESSION_ID(int SESSION_ID) {
        this.SESSION_ID = SESSION_ID;
    }

    public int getSESSION_ID() {
        return this.SESSION_ID;
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj);
    }
}
