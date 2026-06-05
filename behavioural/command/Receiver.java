package behavioural.command;

public class Receiver {
    public void performAction() {
        System.out.println("Receiver: Performing action");
    }

    public void undoAction() {
        System.out.println("Receiver: Undoing action");
    }
}