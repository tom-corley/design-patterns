package behavioural.observer;

public class Main {
    public static void main(String[] args) {
        ConcreteSubject subject = new ConcreteSubject();
        ConcreteEmailObserver emailObserver = new ConcreteEmailObserver();
        ConcreteSMSObserver smsObserver = new ConcreteSMSObserver();
        subject.addObserver(emailObserver);
        subject.addObserver(smsObserver);
        subject.notifyObservers("Hello, world!");
    }
}