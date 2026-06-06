package behavioural.strategy;

public class ConcreteStrategy implements Strategy {
    @Override
    public void execute() {
        System.out.println("Executing primary strategy");
    }
}