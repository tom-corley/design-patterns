package creational.factory;

public class Bowl extends BaseProduct {
    public Bowl(String name, double price) {
        super(name, price);
    }

    @Override
    public void printProduct() {
        System.out.println("Bowl: " + getName() + ", Price: " + getPrice());
    }
}
