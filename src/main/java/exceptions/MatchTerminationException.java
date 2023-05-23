package exceptions;

public class MatchTerminationException extends Exception
{
    private final String m_MatchIdentifier;

    public MatchTerminationException(String i_MatchIdentifier, String i_ExceptionMessage)
    {
        super(i_ExceptionMessage);
        this.m_MatchIdentifier = i_MatchIdentifier;
    }

    public String GetMatchIdentifier()
    {
        return this.m_MatchIdentifier;
    }
}
