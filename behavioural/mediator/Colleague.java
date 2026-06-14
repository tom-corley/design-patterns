package behavioural.mediator;

public abstract class Colleague {
    protected Mediator mediator;

    protected Colleague(Mediator mediator) {
        this.mediator = mediator;
    }

    public void setMediator(Mediator mediator) {
        this.mediator = mediator;
    }

    public void notify(String event) {
        mediator.notify(this, event);
    }
}