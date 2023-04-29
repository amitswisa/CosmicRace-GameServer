package dto;

import addons.Character;

public class CharacterLoginData {

    private final String Message;
    private final String Username;
    private final boolean Success;
    private final String Token;
    private final int Coins;
    private final String Level;
    private final Character Character;

    public CharacterLoginData(String message, String username,
                              boolean success, String token, int coins,
                              String level, addons.Character character) {
        Message = message;
        Username = username;
        Success = success;
        Token = token;
        Coins = coins;
        Level = level;
        Character = character;
    }
}
