package behavioural.mediator;

public interface Mediator {
    void notify(Colleague sender, String event);
}