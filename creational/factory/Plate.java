package creational.factory;

public class Plate extends BaseProduct {
    public Plate(String name, double price) {
        super(name, price);
    }

    @Override
    public void printProduct() {
        System.out.println("Plate: " + getName() + ", Price: " + getPrice());
    }
}
