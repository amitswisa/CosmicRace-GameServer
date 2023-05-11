package match_making;

import player.Player;
import interfaces.Match;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CouchMatch extends Thread implements Match {

    private List<Player> players;
    private String identifyer;
    private boolean gameOver;
    private static final Object RemoveLock = new Object();
    /*private static void insertPlayerToCustomRoom(Player socket){ //assumption: the player inserts game number.
        boolean isPlayerWaitingInMatch = false;
        String gameCode = "";

        try {
            BufferedReader in = socket.getIn_stream();
            PrintWriter out = socket.getOut_stream();

            // collect all the input that the client send in the past 200ms.
            if (in.ready()) {
                gameCode = in.readLine(); // Read last line.
                LoggerManager.info(socket.getPlayerName() + ": my requested gameCode is: " + gameCode);

                //if this is the first time that someone inserts this gameCode, create a new room.
                if(!waitingRoomList.containsKey(gameCode)){
                    waitingRoomList.put(gameCode, new ArrayList<>());
                }

                waitingRoomList.get(gameCode).add(socket);
                isPlayerWaitingInMatch = true;
                LoggerManager.info(socket.getPlayerName() + ": there are " + waitingRoomList.get(gameCode).size()
                        +  "players in the room which his number is: " + gameCode);

                if(waitingRoomList.get(gameCode).size() == Utils.MAXIMUM_AMOUNT_OF_PLAYERS){
                    Match newMatch = new MultiPlayerMatch(gameCode, waitingRoomList.get(gameCode));

                    LoggerManager.info("New match! Players: ");
                    waitingRoomList.get(gameCode).forEach(player -> LoggerManager.info(player.getPlayerName()));

                    new Thread((Runnable) newMatch).start();
                }
            }
        }catch(IOException e){
            LoggerManager.error("something went wrong while trying to create new room.");

            if(isPlayerWaitingInMatch){
                removePlayerFromCustomWaitingList(socket, gameCode);
            }
        }
    }*/
    public static volatile Map<String, List<Player>> waitingRoomList = new HashMap<>();

    @Override
    public String GetMatchIdentifier() {
        return null;
    }

    @Override
    public void EndMatch() {

    }

    @Override
    public boolean IsGameOver() {
        return false;
    }

    @Override
    public void RemovePlayerFromMatch(Player player) {

    }

    @Override
    public void SendToAll(String message) {

    }

    public static void removePlayerFromWaitingList(Player player, String gameCode){
        synchronized (RemoveLock) {
            waitingRoomList.get(gameCode).remove(player);
        }
    }
}
