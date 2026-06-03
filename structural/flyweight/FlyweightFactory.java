package structural.flyweight;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FlyweightFactory {
    private final int id; 
    private final Map<String, Flyweight> flyweights;

    public FlyweightFactory() {
        this.id = new Random().nextInt(1000);
        this.flyweights = new HashMap<>();
    }

    public Flyweight getFlyweight(String key) {
        return flyweights.get(key);
    }

    public void addFlyweight(String key, Flyweight flyweight) {
        flyweights.put(key, flyweight);
    }
}