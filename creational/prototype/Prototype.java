package creational.prototype;

import java.util.Random;

public class Prototype {
    private final int id1;
    private final int id2;
    private final int id3;

    public Prototype() {
        this.id1 = new Random().nextInt(1000);
        this.id2 = new Random().nextInt(1000);
        this.id3 = new Random().nextInt(1000);
    }

    private Prototype(Prototype prototype) {
        this.id1 = prototype.id1;
        this.id2 = prototype.id2;
        this.id3 = prototype.id3;
    }

    public Prototype clone() {
        return new Prototype(this);
    }

    public void printIds() {
        System.out.println("Prototype ids: " + id1 + ", " + id2 + ", " + id3);
    }
}