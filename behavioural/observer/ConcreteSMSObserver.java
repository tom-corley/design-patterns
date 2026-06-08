package behavioural.observer;

public class ConcreteSMSObserver implements Observer {
    @Override
    public void update(String message) {
        System.out.println("SMSObserver: " + message);
    }
}