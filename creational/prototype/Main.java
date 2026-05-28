package creational.prototype;

public class Main {
    public static void main(String[] args) {
        Prototype prototype = new Prototype();
        prototype.printIds();
        Prototype clone = prototype.clone();
        clone.printIds();
        System.out.println("Showing if prototype == clone: " + (prototype == clone));
    }
}