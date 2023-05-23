package utils.route;

public class APIRoutes {

    private final static String HOST_BASE_URL = "http://cosmicrace.tech:6829";
    public static final String FETCH_CHARACTER_ROUTE = "/fetchCharacterData";

    public static String GetCompleteRoute(String i_Route)
    {
        return HOST_BASE_URL + i_Route;
    }
}
