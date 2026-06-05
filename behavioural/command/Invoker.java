package behavioural.command;

import java.util.ArrayDeque;
import java.util.Deque;

public class Invoker {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("No commands to undo");
            return;
        }
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("No commands to redo");
            return;
        }
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
    }
}