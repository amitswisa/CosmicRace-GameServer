package utils.logs;

public class MatchLogger {

    public static void Info(String i_MatchIdentifier, String i_LogMessage)
    {
        String logMessage = "Match #" + i_MatchIdentifier + " log: " + i_LogMessage;
        LoggerManager.info(logMessage);
    }

    public static void Error(String i_MatchIdentifier, String i_LogMessage)
    {
        String logMessage = "Match #" + i_MatchIdentifier + " log: " + i_LogMessage;
        LoggerManager.error(logMessage);
    }

    public static void Debug(String i_MatchIdentifier, String i_LogMessage)
    {
        String logMessage = "Match #" + i_MatchIdentifier + " log: " + i_LogMessage;
        LoggerManager.debug(logMessage);
    }

}
