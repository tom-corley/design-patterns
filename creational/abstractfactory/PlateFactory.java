package creational.abstractfactory;

import creational.factory.BaseProduct;
import creational.factory.Plate;

public class PlateFactory implements Factory {
    private final int id;

    public PlateFactory(int id) {
        this.id = id;
    }

    @Override
    public BaseProduct createProduct(String name, double price) {
        System.out.println("PlateFactory id: " + id + " creating Plate: " + name + ", Price: " + price);
        return new Plate(name, price);
    }
}