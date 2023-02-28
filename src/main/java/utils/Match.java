package utils;

import client.GameSession;
import java.util.List;
import java.util.TimerTask;

public class Match extends TimerTask {

    private List<GameSession> players;

    public Match(List<GameSession> players) {
        this.players = players;
    }

    // Send message to every socket connected to the server.
    public void broadcast(String message) {
        players.forEach((session) -> {
            session.send(message);
        });
    }

    @Override
    public void run() {

    }
}
