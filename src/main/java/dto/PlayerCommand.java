package dto;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import utils.json.JsonFormatter;
import utils.player.AttackInfo;
import utils.player.Location;

public class PlayerCommand {

    private String m_MessageType;
    private String m_Username;
    private Location m_Location;
    private String m_Action;
    private AttackInfo m_AttackInfo;

    public PlayerCommand(String i_MessageType, String i_Username, String i_Action, Location i_Location) {
        this.m_MessageType = i_MessageType;
        this.m_Username = i_Username;
        this.m_Action = i_Action;
        this.m_Location = i_Location;
    }

    public PlayerCommand(){}

    public String GetMessageType()
    {
        return this.m_MessageType.toString();
    }

    public String GetUsername() {
        return this.m_Username;
    }

    public String GetAction() {
        return this.m_Action;
    }

    public Location GetLocation() {
        return this.m_Location;
    }

    public void SetUsername(String m_Username) {
        this.m_Username = m_Username;
    }

    public void SetAction(String m_Action) {
        this.m_Action = m_Action;
    }

    public void SetLocation(Location m_Location) {
        this.m_Location = m_Location;
    }

    public void SetMessageType(String m_MessageType) {
        this.m_MessageType = m_MessageType;
    }

    public void SetAttackInfo(AttackInfo i_AttackInfo){
        this.m_AttackInfo = i_AttackInfo;
    }

    public void ParseFromJson(String jsonString) {
        Gson gson = new Gson();
        JsonElement jsonElement = gson.fromJson(jsonString, JsonElement.class);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        this.m_MessageType = jsonObject.get("m_MessageType").getAsString();
        this.m_Username = jsonObject.get("m_Username").getAsString();
        this.m_Action = jsonObject.get("m_Action").getAsString();

        JsonObject locationJson = jsonObject.getAsJsonObject("m_Location");
        this.m_Location = new Location(locationJson.get("x").getAsDouble(), locationJson.get("y").getAsDouble());
    }

    public static String Serialize(PlayerCommand i_Command)
    {
        return JsonFormatter.GetGson().toJson(i_Command, PlayerCommand.class);
    }
}