package structural.adapter;

public class Client {
    private final Target target;

    public Client(Target target) {
        this.target = target;
    }

    public int divide(int a, int b) {
        return target.divide(a, b);
    }
}