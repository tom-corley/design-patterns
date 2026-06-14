package behavioural.memento;

import java.util.ArrayDeque;
import java.util.Deque;

public class GameManager {
    private final GameSave gameSave;
    private final Deque<GameSave.GameSaveMemento> undoStack = new ArrayDeque<>();
    private final Deque<GameSave.GameSaveMemento> redoStack = new ArrayDeque<>();

    public GameManager(GameSave gameSave) {
        this.gameSave = gameSave;
    }

    public void save() {
        undoStack.push(gameSave.createMemento());
        redoStack.clear();
    }
    
    public void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("No save to undo");
            return;
        }
        GameSave.GameSaveMemento memento = undoStack.pop();
        redoStack.push(memento);
        gameSave.restore(memento);
    }
    
    public void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("No save to redo");
            return;
        }
        GameSave.GameSaveMemento memento = redoStack.pop();
        undoStack.push(memento);
        gameSave.restore(memento);
    }
}