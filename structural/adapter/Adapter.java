package structural.adapter;

public class Adapter implements Target {
    private final Adaptee adaptee;
    public Adapter(Adaptee adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public int divide(int a, int b) {
        return (int) adaptee.divideAByB(a, b);
    }
}