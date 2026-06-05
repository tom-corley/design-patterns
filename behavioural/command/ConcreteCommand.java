package behavioural.command;

public class ConcreteCommand implements Command {
    private final Receiver receiver;

    public ConcreteCommand(Receiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void execute() {
        receiver.performAction();
    }

    @Override 
    public void undo() {
        receiver.undoAction();
    }
}