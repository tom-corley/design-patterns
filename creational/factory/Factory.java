package creational.factory;

import java.util.Random;

public class Factory {
    private final int id;

    public Factory() {
        this.id = new Random().nextInt(1000);
    }

    public BaseProduct createBowl(String name, double price) {
        return new Bowl(name, price);
    }

    public BaseProduct createPlate(String name, double price) {
        return new Plate(name, price);
    }

    public void printId() {
        System.out.println("Factory id: " + id);
    }
}