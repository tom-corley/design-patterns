package behavioural.state;

public class Done implements State {
    @Override
    public void doAction(Context context) {
        throw new IllegalStateException("Task is already done, cannot do action");
    }
}