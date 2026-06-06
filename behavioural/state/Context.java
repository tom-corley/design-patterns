package behavioural.state;

public class Context {
    private State state;

    public Context(State state) {
        this.state = state;
    }

    public void doAction() {
        state.doAction(this);
    }

    public void setState(State state) {
        this.state = state;
    }
}