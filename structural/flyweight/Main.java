package structural.flyweight;

public class Main {
    public static void main(String[] args) {
        FlyweightFactory factory = new FlyweightFactory();
        Flyweight flyweight = factory.getFlyweight(1);
        Flyweight flyweight2 = factory.getFlyweight(1);
        flyweight2.operation(4);
        flyweight.operation(2);
        flyweight.operation(3);
        flyweight.operation(2);
        flyweight.operation(3);
    }
}