package structural.adapter;

public class Adaptee {
    public Adaptee() {}

    // Legacy calculator using doubles
    public double divideAByB(double a, double b) {
        return a / b;
    }
}