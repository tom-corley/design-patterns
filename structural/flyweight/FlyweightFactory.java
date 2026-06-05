package structural.flyweight;

import java.util.HashMap;
import java.util.Map;

public class FlyweightFactory {
    private final Map<Integer, Flyweight> flyweights;

    public FlyweightFactory() {
        this.flyweights = new HashMap<>();
    }

    public Flyweight getFlyweight(int key) {
        if (flyweights.containsKey(key)) {
            return flyweights.get(key);
        } else {
            Flyweight flyweight = new ConcreteFlyweight(key);
            flyweights.put(key, flyweight);
            return flyweight;
        }
    }
}