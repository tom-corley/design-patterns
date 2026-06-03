package structural.decorator;

public class Trilby implements Hat {
    @Override
    public void putOn() {
        System.out.println("Putting on trilby");
    }

    @Override
    public void takeOff() {
        System.out.println("Taking off trilby");
    }

    @Override
    public int getCost() {
        return 100;
    }

    @Override
    public String toString() {
        return "Trilby";
    }
}