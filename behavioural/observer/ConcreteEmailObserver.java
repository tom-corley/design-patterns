package behavioural.observer;

public class ConcreteEmailObserver implements Observer {
    @Override
    public void update(String message) {
        System.out.println("EmailObserver: " + message);
    }
}