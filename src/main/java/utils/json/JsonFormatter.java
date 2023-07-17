package utils.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonFormatter {

    private final static Gson gson = new Gson();

    public static JsonObject createJsonFromString(String json){
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        return jsonElement.getAsJsonObject();
    }

    public JsonElement stringToJsonElement(String jsonString) {
        // Parsing the string into a JsonElement
        return GetGson().fromJson(jsonString, JsonElement.class);
    }


    public static Gson GetGson() {
        return gson;
    }
}
