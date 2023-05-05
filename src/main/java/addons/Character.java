package addons;

public class Character {
   private int characterID;
   private String characterName;
   private int level;
   private int xp;
   private int power;
   private int magicPoints;
   private float speed;
   private float defence;
   private float jump;
   private int wins;
   private int loses;

    public Character(int characterID, String characterName, int level, int xp,
                     int power, int magicPoints, float speed, float defence,
                     float jump, int wins, int loses) {

        this.characterID = characterID;
        this.characterName = characterName;
        this.level = level;
        this.xp = xp;
        this.power = power;
        this.magicPoints = magicPoints;
        this.speed = speed;
        this.defence = defence;
        this.jump = jump;
        this.wins = wins;
        this.loses = loses;
    }

    public int getCharacterID() {
        return characterID;
    }

    public String getCharacterName() {
        return characterName;
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public int getPower() {
        return power;
    }

    public int getMagicPoints() {
        return magicPoints;
    }

    public float getSpeed() {
        return speed;
    }

    public float getDefence() {
        return defence;
    }

    public float getJump() {
        return jump;
    }

    public int getWins() {
        return wins;
    }

    public int getLoses() {
        return loses;
    }
}
