package creational.abstractfactory;

import java.util.Random;

public class AbstractFactory {
    private final int id;

    public AbstractFactory() {
        this.id = new Random().nextInt(1000);
    }

    public Factory createBowlFactory() {
        return new BowlFactory(id);
    }

    public Factory createPlateFactory() {
        return new PlateFactory(id);
    }
}
