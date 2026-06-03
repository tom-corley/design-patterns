package structural.bridge;

public abstract class Abstraction {
    protected final Implementation implementation;

    protected Abstraction(Implementation implementation) {
        this.implementation = implementation;
    }

    public abstract void notify(String message);
}