package utils;

import client.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class Match extends Thread {

    private List<Player> players;
    private String identifyer;
    private TimerTask clientsDataRefresher;
    private Timer timer;
    private boolean gameOver;


    public Match(String identifyer, List<Player> players) {
        // Update current match for all players.
        this.identifyer = identifyer;
        this.players = players;
        this.players.forEach(player -> player.setNewMatch(this));

        // Game start!
        this.gameOver = false;
    }

    @Override
    public void run() {

        // Log match started
        this.broadCastToAll("Match Started!");

        // Keep game alive until its over.
        while(!gameOver)
        {
            // should read the input from socket's buffer for each player.
            for (Player player : players) {
                try {
                    BufferedReader in = player.getIn_stream();
                    PrintWriter out = player.getOut_stream();

                    // collect all the input that the client send in the past 200ms.
                    if (in.ready()) {
                        String inputLine = in.readLine(); // Read last line.
//                        in.lines();
                        LoggerManager.info(player.getPlayerName() + ": " + inputLine);

                        //TODO: store client's data somehow, and update his process.
                        out.println("Data received.");
                    }

                } catch (IOException e) {
                    LoggerManager.debug("Something went wrong with player " + player.getPlayerName());
                    throw new RuntimeException(e);
                }
            }

            try {
                // collecting data from client each 200ms.
                this.broadCastToAll("Players Update!");
                LoggerManager.info("Going to sleep 200ms.");
                Thread.sleep(200);
            } catch (InterruptedException e) {
                LoggerManager.error(e.getMessage());
                throw new RuntimeException(e);
            }

        }

        endMatch();
    }

    public boolean isGameOver() {
        return this.gameOver;
    }

    public void removePlayerFromMatch(Player player) {
        this.players.remove(player);

        // if there's only one player left in the game.
        if(players.size() == 1)
            this.gameOver = true;

        LoggerManager.info("Player " + player.getPlayerName() + " has left the game!");
        this.broadCastToAll("Player " + player.getPlayerName() + " has left the game!"); // Send player left message to remaining players.
    }

    public void endMatch() {
        // Set all player's current match to null.
        LoggerManager.info("Match ended!");

        // TODO - update players coins and stats on database.

        MatchMaking.removeActiveMatch(this);
        this.players.forEach(player -> player.closeConnection());
    }


    public void broadCastToAll(String message){
        players.stream().forEach(player -> player.sendMessage(message));
        LoggerManager.info("Message to all players: " + message);
    }

    public String getIdentifyer() {
        return this.identifyer;
    }



//    public void collectClientsData() {
//        clientsDataRefresher = new ClientsDataRefresher(players,
//                this::doSomething);
//        timer = new Timer();
//        timer.schedule(clientsDataRefresher, 200,200);
//    }
//
//    private void doSomething(){
//
//    }
}
