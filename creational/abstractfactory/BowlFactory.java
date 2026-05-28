package creational.abstractfactory;

import creational.factory.BaseProduct;
import creational.factory.Bowl;

public class BowlFactory implements Factory {
    private final int id;

    public BowlFactory(int id) {
        this.id = id;
    }

    @Override
    public BaseProduct createProduct(String name, double price) {
        System.out.println("BowlFactory id: " + id + " creating Bowl: " + name + ", Price: " + price);
        return new Bowl(name, price);
    }
}