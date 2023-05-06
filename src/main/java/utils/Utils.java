package utils;

import addons.Character;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.DriverManager;

public class Utils {
    public static final Gson gson = new Gson();

    public static final String HOST = "localhost";
    public static final int PORT = 6666;
    public static final String AUTH_KEY = "9c62e986-5d00-44f6-97be-59c9f2207852";
    public static final Integer MAXIMUM_AMOUNT_OF_PLAYERS = 1;
    // Messages
    public static final String UN_AUTHORIZED = "Unauthorized access, connection closed!";

    //DB:
    public final static String DB_URL = "";
    public final static String DB_USERNAME = "admin_cosmicrace";
    public final static String DB_PASSWORD = "cosmicrace!@#";


    public final static String WEB_API_URL = "http://cosmicrace.tech:6829";
    public static void printCharacter(Character character){

        System.out.println("Character ID: " + character.getCharacterID());
        System.out.println("Character Name: " + character.getCharacterName());
        System.out.println("Level: " + character.getLevel());
        System.out.println("XP: " + character.getXp());
        System.out.println("Magic Points: " + character.getMagicPoints());
        System.out.println("Speed: " + character.getSpeed());
        System.out.println("Power: " + character.getPower());
        System.out.println("Defense: " + character.getDefence());
        System.out.println("Jump: " + character.getJump());
        System.out.println("Wins: " + character.getWins());
        System.out.println("Loses: " + character.getLoses());
    }

    public static JsonObject createJsonFromString(String json){
        Gson gson = new Gson();
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        return jsonObject;
    }

}

