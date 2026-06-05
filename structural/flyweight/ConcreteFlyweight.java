package structural.flyweight;

public class ConcreteFlyweight implements Flyweight {
    private final int intrinsicState;

    public ConcreteFlyweight(int intrinsicState) {
        // Expensive creation logic
        // Pretend this is also a lot of data
        this.intrinsicState = intrinsicState;
    }

    @Override
    public void operation(int extrinsicState) {
        System.out.println("ConcreteFlyweight: " + intrinsicState + " " + extrinsicState);
    }
}
