package utils.logs;

public class MatchLogger {

    public enum LogType {
        PLAYER_MESSAGE, ALL_MESSAGE, NOTIFICATION, WARNING
    }

    public static void Info(String i_MatchIdentifier,LogType logType, String i_LogMessage)
    {
        String logMessage = "\nMatch Log - #(" + i_MatchIdentifier + ")." +
                "\nLog type: " + logType.toString() +
                "\nLog Message: " + i_LogMessage;

        LoggerManager.info(logMessage);
    }

}
