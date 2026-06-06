package behavioural.strategy;

public class AlternateStrategy implements Strategy {
    @Override
    public void execute() {
        System.out.println("Executing alternate strategy");
    }
}