package behavioural.state;

public class Pending implements State {
    @Override
    public void doAction(Context context) {
        System.out.println("Task is pending");
        context.setState(new InProgress());
    }
}   