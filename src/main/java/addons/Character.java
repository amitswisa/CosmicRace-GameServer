package addons;

public class Character {

    // TODO - Everything

    private float strength;
    private float speed;
    private float power;
    private int amountOfCoins;

    public Character(float strength, float speed, float power, int amountOfCoins) {
        this.strength = strength;
        this.speed = speed;
        this.power = power;
        this.amountOfCoins = amountOfCoins;
    }

    public float getStrength() {
        return strength;
    }

    public float getSpeed() {
        return speed;
    }

    public float getPower() {
        return power;
    }

    public int getAmountOfCoins() {
        return amountOfCoins;
    }

    public int increaseAmountOfCoins(int amountOfCoins) {
        ++amountOfCoins;
        this.amountOfCoins = amountOfCoins;
        return this.amountOfCoins;
    }
}
