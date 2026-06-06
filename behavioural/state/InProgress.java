package behavioural.state;

public class InProgress implements State {
    @Override
    public void doAction(Context context) {
        System.out.println("Task is in progress");
        context.setState(new Done());
    }
}