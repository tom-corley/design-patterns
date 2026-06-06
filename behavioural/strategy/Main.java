package behavioural.strategy;

public class Main {
    public static void main(String[] args) {
        Strategy strategy1 = new ConcreteStrategy();
        Strategy strategy2 = new AlternateStrategy();
        Context context = new Context(strategy1);
        context.executeStrategy();
        context.setStrategy(strategy2);
        context.executeStrategy();
    }
}