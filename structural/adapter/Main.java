package structural.adapter;

public class Main {
    public static void main(String[] args) {
        Adaptee adaptee = new Adaptee();
        Target target = new Adapter(adaptee);
        Client client = new Client(target);
        System.out.println(client.divide(10, 2));
    }
}