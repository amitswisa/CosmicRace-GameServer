package addons;

public class Character {
   private int CharacterID;
   private String CharacterName;
   private int Level;
   private int xp;
   private int Power;
   private int MagicPoints;
   private float Speed;
   private float Defence;
   private float jump;
   private int Wins;
   private int Loses;

    public Character(int characterID, String characterName, int level,
                     int xp, int magicPoints, float speed, float defence, float jump,
                     int wins, int loses, int power) {
        CharacterID = characterID;
        CharacterName = characterName;
        Level = level;
        this.xp = xp;
        MagicPoints = magicPoints;
        Speed = speed;
        Defence = defence;
        this.jump = jump;
        Wins = wins;
        Loses = loses;
        Power = power;
    }

    public int getCharacterID() {
        return CharacterID;
    }

    public String getCharacterName() {
        return CharacterName;
    }

    public int getLevel() {
        return Level;
    }

    public int getXp() {
        return xp;
    }

    public int getMagicPoints() {
        return MagicPoints;
    }

    public float getSpeed() {
        return Speed;
    }

    public float getDefence() {
        return Defence;
    }

    public float getJump() {
        return jump;
    }

    public int getPower() {
        return Power;
    }

    public int getWins() {
        return Wins;
    }

    public int getLoses() {
        return Loses;
    }


    public void setCharacterID(int characterID) {
        CharacterID = characterID;
    }

    public void setCharacterName(String characterName) {
        CharacterName = characterName;
    }

    public void setLevel(int level) {
        Level = level;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public void setPower(int power) {
        Power = power;
    }

    public void setMagicPoints(int magicPoints) {
        MagicPoints = magicPoints;
    }

    public void setSpeed(int speed) {
        Speed = speed;
    }

    public void setDefence(int defence) {
        Defence = defence;
    }

    public void setJump(int jump) {
        this.jump = jump;
    }

    public void setWins(int wins) {
        Wins = wins;
    }

    public void setLoses(int loses) {
        Loses = loses;
    }
}
