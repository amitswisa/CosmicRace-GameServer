package utils.messages_manager;

import com.google.gson.JsonObject;
import json.JsonFormatter;
import player.Player;

public class MessagesCreator {

    //not quite sure about those fields.
    public static String CreateClientCurrentActions(Player i_Player){
        JsonObject res = new JsonObject();
        res.addProperty("username", i_Player.GetUserName());
//        res.addProperty("location", i_Player.GetCharacter().getLocation());

//        res.addProperty("action", );
        return JsonFormatter.GetGson().toJson(res);
    }
}
