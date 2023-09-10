package exceptions;

public class MatchTerminationException extends Exception
{
    private final String m_MatchIdentifier;
    private final String m_ExceptionMessage;

    public MatchTerminationException(String i_MatchIdentifier, String i_ExceptionMessage)
    {
        this.m_ExceptionMessage = i_ExceptionMessage;
        this.m_MatchIdentifier = i_MatchIdentifier;
    }

    public String GetMatchIdentifier()
    {
        return this.m_MatchIdentifier;
    }

    @Override
    public String getMessage() {
        return this.m_ExceptionMessage;
    }
}
