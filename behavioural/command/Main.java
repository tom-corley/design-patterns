package behavioural.command;

public class Main {
    public static void main(String[] args) {
        Receiver receiver = new Receiver();
        ConcreteCommand command1 = new ConcreteCommand(receiver);
        ConcreteCommand command2 = new ConcreteCommand(receiver);
        Invoker invoker = new Invoker();
        invoker.executeCommand(command1);
        invoker.executeCommand(command2);
        invoker.undo();
        invoker.redo();
    }
}