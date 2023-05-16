package utils;

public class GlobalSettings {

    // General settings
    public static final int PORT = 6666;
    public static final Integer MAXIMUM_AMOUNT_OF_PLAYERS = 1;
    public static final Integer MINIMUM_AMOUNT_OF_PLAYERS = 1;
    public static final int MAX_TIME_OUT = 10000; // 10 Seconds

    // API Routes
    public static final String FETCH_CHARACTER_ROUTE = "";

    // Database
    public final static String DB_URL = "";
    public final static String DB_USERNAME = "admin_cosmicrace";
    public final static String DB_PASSWORD = "cosmicrace!@#";

    // Communication messages
    public final static String HEARTBEAT_MESSAGE = "isAlive\n";
    public final static String CLIENT_HEARTBEAT_RESPONSE = "ALIVE";

    // Player validations messages
    public final static String PLAYER_READY_MESSAGE = "READY?";
    public final static String PLAYER_READY_RESPONSE_MESSAGE = "READY";

    // Player errors
    public final static String COULDNT_FETCH_CHARACTER_DATA_ERROR = "Error occurred while fetching character data.";
    public static final String TERMINATE_DUE_TO_TIME_OUT = "Client socket closed due to reachability issues.";
    public static final String CLIENT_CLOSED_CONNECTION = "Cant reach client, closing connection..";
    public static final String NO_CONNECTION = "No connection";
    public static final String MATCH_ENDED = "Match ended";
    public static final String MATCH_TERMINATED = "Match has been terminated due to unexpected error.";
    public static final String NO_MESSAGES_IN_CLIENT_BUFFER = "No Messages";
}

