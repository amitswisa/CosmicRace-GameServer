package dto;

import com.google.gson.JsonObject;
import utils.json.JsonFormatter;

public class PlayerMatchInfo {
    private final String m_Username;
    private final int m_Position;
    private final int m_Coins;

    public PlayerMatchInfo(String i_Username, int i_Position, int i_Coins) {
        this.m_Username = i_Username;
        this.m_Position = i_Position;
        this.m_Coins = i_Coins;
    }

    @Override
    public String toString() {
        JsonObject res = new JsonObject();
        res.addProperty("Username", this.m_Username);
        res.addProperty("Position", this.m_Position);
        res.addProperty("Coins", this.m_Coins);
        return JsonFormatter.GetGson().toJson(res);
    }
}
