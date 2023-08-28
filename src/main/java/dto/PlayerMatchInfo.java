package dto;

import com.google.gson.JsonObject;
import utils.json.JsonFormatter;

public class PlayerMatchInfo {
    private final String m_Username;
    private final int m_Position;

    public PlayerMatchInfo(String m_Username, int m_Position) {
        this.m_Username = m_Username;
        this.m_Position = m_Position;
    }

    @Override
    public String toString() {
        JsonObject res = new JsonObject();
        res.addProperty("Username", this.m_Username);
        res.addProperty("Position", this.m_Position);
        return JsonFormatter.GetGson().toJson(res);
    }
}
