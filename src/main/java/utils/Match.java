package utils;

import client.Player;
import java.util.List;
import java.util.logging.Logger;

public class Match extends Thread {

    private List<Player> players;

    private String identifyer;

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

        LoggerManager.info("Match Started!");

        // Keep game alive until its over.
        while(!gameOver)
        {

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

    }

    public String getIdentifyer() {
        return this.identifyer;
    }

}
