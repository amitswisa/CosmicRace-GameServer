package utils;

public class GlobalSettings {

    // General settings
    public static final Integer MAXIMUM_AMOUNT_OF_PLAYERS = 3;
    public static final Integer MINIMUM_AMOUNT_OF_PLAYERS = 2;
    public static final int MAX_TIME_OUT = 10000; // 10 Seconds
    public static final long ATTACK_COOLDOWN = 5000; // 5 Seconds

    // API Routes
    public static final String FETCH_CHARACTER_ROUTE = "";

    // Database
    public final static String DB_URL = "";
    public final static String DB_USERNAME = "admin_cosmicrace";
    public final static String DB_PASSWORD = "cosmicrace!@#";

    // Communication messages
    public final static String SERVER_HEARTBEAT_MESSAGE = "isAlive\n";
    public final static String CLIENT_HEARTBEAT_RESPONSE = "ALIVE";
    public final static String CLIENT_QUITED_BY_CHOICE = "QUIT";

    // Player validations messages
    public final static String PLAYER_READY_MESSAGE = "READY?";
    public final static String PLAYER_READY_RESPONSE_MESSAGE = "READY";

    // Player errors
    public final static String COULDNT_FETCH_CHARACTER_DATA_ERROR = "Error occurred while fetching character data.";
    public static final String TERMINATE_DUE_TO_TIME_OUT = "Client socket closed due to reachability issues.";
    public static final String CLIENT_CLOSED_CONNECTION = "The client closed the connection.";
    public static final String NO_CONNECTION = "No connection";
    public static final String MATCH_ENDED = "Match ended";
    public static final String HOST_CLOSED_MATCH = "Host closed the game session.";
    public static final String MATCH_TERMINATED = "Match has been stopped due to unexpected error.";
    public static final String NO_MESSAGES_IN_CLIENT_BUFFER = "No Messages";
    public static final String NOT_ENOUGH_PLAYERS_TO_CONTINUE = "Match has been terminated - Number of players is under the minimum required.";
}

