package creational.singleton;

import java.util.Random;

public class Singleton {
    private static Singleton instance;
    private final int id;

    private Singleton() {
        this.id = new Random().nextInt(1000);
    }

    public static Singleton getSingleton() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }

    public void printId() {
        System.out.println("Singleton id: " + id);
    }
    
}
