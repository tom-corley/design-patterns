package creational.factory;

public class Main {
    public static void main(String[] args) {
        Factory factory = new Factory();
        factory.printId();
        factory.createBowl("Bowl 1", 10.0).printProduct();
        factory.createPlate("Plate 1", 20.0).printProduct();
    }
}