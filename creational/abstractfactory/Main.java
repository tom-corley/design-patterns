package creational.abstractfactory;

public class Main {
    public static void main(String[] args) {
        AbstractFactory abstractFactory = new AbstractFactory();
        Factory bowlFactory = abstractFactory.createBowlFactory();
        Factory plateFactory = abstractFactory.createPlateFactory();
        bowlFactory.createProduct("Bowl 1", 10.0);
        plateFactory.createProduct("Plate 1", 20.0);
    }
}