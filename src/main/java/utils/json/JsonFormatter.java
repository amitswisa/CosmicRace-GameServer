package utils.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonFormatter {

    private final static Gson gson = new Gson();

    public static JsonObject createJsonFromString(String json){
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        return jsonElement.getAsJsonObject();
    }

    public static Gson GetGson() {
        return gson;
    }
}
