# Memento Pattern

**Classification:** Behavioural (GoF)

**Also known as:** Token, Snapshot

---

## 1. Core Idea

The Memento pattern captures an object's internal state at a point in time so that it can be restored to that state later, all without exposing the internals of the object to the outside world.

### The Problem

Consider a text editor. Users expect to press Ctrl+Z and undo their last change -- and then undo the change before that, and so on. To support this, the editor needs to save snapshots of the document's state (text content, cursor position, selection, formatting) at various moments in time.

The naive approach is to let the undo manager reach into the document object and copy its fields directly. But this forces the document to expose every piece of internal state through public getters, which violates encapsulation. If the document's internal representation changes (say, from a flat string to a piece table), every consumer of those getters breaks.

Alternatively, you could make the document itself responsible for storing and managing its history, but that conflates two responsibilities: editing content and managing undo history. The document class becomes bloated and hard to reason about.

### The Key Insight

Have the object whose state needs saving (the Originator) produce an opaque snapshot of itself -- a Memento -- that only the Originator knows how to interpret. An external object (the Caretaker) can hold onto these mementos and hand them back when a restore is needed, but it never looks inside them. The memento is a black box to everyone except its creator.

This preserves encapsulation: the Originator's internal structure is never exposed, the Caretaker has no coupling to state details, and responsibility for history management is cleanly separated from the domain logic.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Originator** | The object whose state needs to be saved and restored. Creates mementos containing a snapshot of its current state. Can restore itself from a given memento. |
| **Memento** | A value object that stores a snapshot of the Originator's internal state. Ideally immutable. Provides no public setters and exposes its contents only to the Originator (in Java, typically achieved via a nested class or package-private access). |
| **Caretaker** | Manages memento lifecycle -- requests mementos from the Originator, stores them (e.g. in a stack for undo), and passes them back to the Originator when a restore is needed. The Caretaker never inspects or modifies the memento's contents. |

### Relationships

```
  +------------------------+          +-------------------------+
  |      Caretaker         |          |       Originator        |
  |------------------------|          |-------------------------|
  | - history: List<M>     |          | - state (private)       |
  |                        |          |                         |
  | + save()               |--------->| + createMemento(): M    |
  | + undo()               |--------->| + restore(M): void      |
  +------------------------+          +-------------------------+
                                                |
                                                | creates / reads
                                                v
                                      +-------------------------+
                                      |        Memento          |
                                      |-------------------------|
                                      | - savedState (private)  |
                                      |                         |
                                      | (accessible only to     |
                                      |  Originator)            |
                                      +-------------------------+
```

**The flow for saving state:**
1. The Caretaker calls `originator.createMemento()`.
2. The Originator creates a new Memento containing a copy of its current internal state.
3. The Caretaker stores the Memento (typically pushing it onto a history stack).

**The flow for restoring state:**
1. The Caretaker pops a Memento from its history.
2. The Caretaker calls `originator.restore(memento)`.
3. The Originator reads the saved state from the Memento and applies it to itself.

At no point does the Caretaker access the state inside the Memento.

---

## 3. Use Cases

**Undo/redo in editors and creative tools.** A text editor, image editor, or CAD tool saves a snapshot before each user action. The undo stack holds mementos; pressing undo pops the most recent one and restores the document. Redo is implemented by maintaining a second stack of mementos displaced by undo operations.

**Savepoints in database transactions.** A database connection allows you to mark a savepoint within a transaction. If a later operation fails, you roll back to the savepoint rather than aborting the entire transaction. The savepoint is a memento of the transaction's state at that moment.

**Game save states.** A game saves the player's position, inventory, health, and world state to a memento (or serialized form of one). The player can reload from a previous save at any time. The save/load system (Caretaker) does not need to understand the internal game state representation.

**Form wizards with "back" navigation.** A multi-step form captures user input across several pages. When the user clicks "Back", the form restores the inputs from the previous step. Each step's state is captured as a memento before advancing.

**Configuration rollback.** A system configuration manager takes a snapshot before applying changes. If the new configuration causes failures (e.g. a health check fails), it rolls back to the previous snapshot. The rollback mechanism is decoupled from the configuration's internal structure.

**Checkpointing in long-running computations.** A data pipeline or machine-learning training loop periodically saves its progress. If the process crashes, it resumes from the most recent checkpoint rather than restarting from scratch.

---

## 4. Example in Java

This example models a text editor with undo support. The `TextEditor` is the Originator, `EditorMemento` is the Memento (implemented as a nested class to enforce encapsulation), and `EditorHistory` is the Caretaker.

### Memento and Originator

```java
import java.util.Objects;

/**
 * Originator. Holds the mutable state of a text editing session.
 * Can produce and consume opaque mementos of that state.
 */
public class TextEditor {

    private StringBuilder content;
    private int cursorPosition;

    public TextEditor() {
        this.content = new StringBuilder();
        this.cursorPosition = 0;
    }

    // ---- Editing operations ----

    public void type(String text) {
        content.insert(cursorPosition, text);
        cursorPosition += text.length();
    }

    public void delete(int count) {
        if (count <= 0) return;
        int start = Math.max(0, cursorPosition - count);
        content.delete(start, cursorPosition);
        cursorPosition = start;
    }

    public void moveCursor(int position) {
        this.cursorPosition = Math.max(0, Math.min(position, content.length()));
    }

    // ---- Memento operations ----

    /**
     * Creates a memento capturing the editor's current state.
     * The returned object is opaque to external callers.
     */
    public EditorMemento createMemento() {
        return new EditorMemento(content.toString(), cursorPosition);
    }

    /**
     * Restores the editor to the state captured in the given memento.
     */
    public void restore(EditorMemento memento) {
        Objects.requireNonNull(memento, "memento must not be null");
        this.content = new StringBuilder(memento.savedContent);
        this.cursorPosition = memento.savedCursorPosition;
    }

    // ---- Query ----

    public String getContent() {
        return content.toString();
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    @Override
    public String toString() {
        return "TextEditor{content=\"" + content + "\", cursor=" + cursorPosition + "}";
    }

    // ---- Memento (nested class) ----

    /**
     * Memento. Immutable snapshot of the editor's internal state.
     *
     * Declared as a static nested class so that only TextEditor can
     * access the saved fields. External code can hold a reference to
     * an EditorMemento but cannot read or modify its contents.
     */
    public static class EditorMemento {

        private final String savedContent;
        private final int savedCursorPosition;

        private EditorMemento(String savedContent, int savedCursorPosition) {
            this.savedContent = savedContent;
            this.savedCursorPosition = savedCursorPosition;
        }

        // No public getters. The enclosing class (TextEditor) accesses
        // the fields directly, but no other class can.
    }
}
```

### Caretaker

```java
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Caretaker. Maintains a history of mementos and coordinates
 * save/undo operations. Never inspects memento contents.
 */
public class EditorHistory {

    private final TextEditor editor;
    private final Deque<TextEditor.EditorMemento> undoStack = new ArrayDeque<>();
    private final Deque<TextEditor.EditorMemento> redoStack = new ArrayDeque<>();

    public EditorHistory(TextEditor editor) {
        this.editor = editor;
    }

    /**
     * Saves the editor's current state before a change is made.
     * Call this before each user action that should be undoable.
     */
    public void save() {
        undoStack.push(editor.createMemento());
        redoStack.clear(); // new action invalidates the redo history
    }

    /**
     * Undoes the most recent change by restoring the previous state.
     *
     * @return true if an undo was performed, false if there was nothing to undo
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            System.out.println("  (nothing to undo)");
            return false;
        }
        // Save current state to redo stack before restoring
        redoStack.push(editor.createMemento());
        editor.restore(undoStack.pop());
        return true;
    }

    /**
     * Redoes a previously undone change.
     *
     * @return true if a redo was performed, false if there was nothing to redo
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            System.out.println("  (nothing to redo)");
            return false;
        }
        undoStack.push(editor.createMemento());
        editor.restore(redoStack.pop());
        return true;
    }

    public int undoSteps() {
        return undoStack.size();
    }

    public int redoSteps() {
        return redoStack.size();
    }
}
```

### Client code

```java
public class Main {

    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        EditorHistory history = new EditorHistory(editor);

        // Simulate a user typing a sentence
        history.save();
        editor.type("Hello");
        System.out.println("After typing 'Hello':    " + editor);

        history.save();
        editor.type(", world");
        System.out.println("After typing ', world':  " + editor);

        history.save();
        editor.type("!");
        System.out.println("After typing '!':        " + editor);

        // Undo the last three changes
        System.out.println("\n--- Undo ---");

        history.undo();
        System.out.println("After 1st undo:          " + editor);

        history.undo();
        System.out.println("After 2nd undo:          " + editor);

        history.undo();
        System.out.println("After 3rd undo:          " + editor);

        // Redo two steps
        System.out.println("\n--- Redo ---");

        history.redo();
        System.out.println("After 1st redo:          " + editor);

        history.redo();
        System.out.println("After 2nd redo:          " + editor);

        // New action after redo clears the remaining redo history
        System.out.println("\n--- New action after redo ---");

        history.save();
        editor.type(" -- updated");
        System.out.println("After typing ' -- updated': " + editor);
        System.out.println("Redo steps available: " + history.redoSteps());
    }
}
```

### Expected output

```
After typing 'Hello':    TextEditor{content="Hello", cursor=5}
After typing ', world':  TextEditor{content="Hello, world", cursor=12}
After typing '!':        TextEditor{content="Hello, world!", cursor=13}

--- Undo ---
After 1st undo:          TextEditor{content="Hello, world", cursor=12}
After 2nd undo:          TextEditor{content="Hello", cursor=5}
After 3rd undo:          TextEditor{content="", cursor=0}

--- Redo ---
After 1st redo:          TextEditor{content="Hello", cursor=5}
After 2nd redo:          TextEditor{content="Hello, world", cursor=12}

--- New action after redo ---
After typing ' -- updated': TextEditor{content="Hello, world -- updated", cursor=23}
Redo steps available: 0
```

### Why this works

- **Encapsulation is preserved.** `EditorMemento` has a private constructor and no public getters. The `EditorHistory` (Caretaker) holds mementos but cannot read or modify the saved content or cursor position. Only `TextEditor` accesses the memento's fields.
- **Single Responsibility.** The editor handles editing. The history handles undo/redo bookkeeping. Neither is burdened with the other's concerns.
- **Redo support comes naturally.** By saving the current state to a redo stack before restoring an undo memento, bidirectional navigation through history falls out of the same mechanism.
- **Extensibility.** If the editor gains new state (e.g. selection range, scroll position, formatting), only `TextEditor` and `EditorMemento` need to change. The `EditorHistory` class is untouched because it never inspects memento contents.

---

## 5. Tradeoffs and Limitations

### When to use it

- You need to implement undo, rollback, or checkpoint-and-restore functionality.
- The object whose state is being saved has complex internal state that should not be exposed through public getters.
- The Caretaker (the thing managing history) should be decoupled from the Originator's internal representation so that changes to one do not ripple into the other.
- You want a clean separation between "what state to capture" (Originator's concern) and "when and how long to keep it" (Caretaker's concern).

### When not to use it

- **State is trivially small and already public.** If the object's state is a single integer or a plain DTO with public fields, the overhead of a separate Memento class buys you nothing. Just copy the value directly.
- **State changes are easily described as deltas.** If you can represent each change as a reversible command (e.g. "insert 5 characters at position 10" / "delete 5 characters at position 10"), the Command pattern with an undo method is often more memory-efficient than storing full snapshots. Many real undo systems use Command for small operations and Memento for periodic full snapshots (checkpoints).
- **The object graph is deeply nested or contains references to shared mutable objects.** Creating a true deep copy for the memento becomes complex and error-prone. You may need to combine Memento with a serialization strategy or consider the Prototype pattern.

### Memory cost of storing snapshots

The most significant practical concern with Memento is memory consumption. Each memento is a full copy of the Originator's state at that point in time. If the state is large (a multi-megabyte document, a complex game world), and you save frequently, memory usage grows linearly with the number of snapshots.

Mitigation strategies include:

- **Limiting history depth.** Cap the undo stack at N entries and discard the oldest mementos. This is what most editors do.
- **Delta-based compression.** Store only the first memento as a full snapshot and subsequent mementos as diffs from their predecessor. This trades restore speed for memory savings, since restoring an arbitrary point requires replaying diffs from the nearest full snapshot.
- **Copy-on-write or structural sharing.** If the state is a tree or map, use persistent (immutable) data structures so that mementos share unchanged subtrees rather than duplicating them. This is the approach used by many functional-programming-inspired undo systems.
- **Hybrid approach.** Use Command pattern for individual operations (cheap to store) and take full Memento snapshots at intervals as checkpoints. To undo, reverse commands back to the nearest checkpoint.

### Serialization concerns

If mementos need to survive process restarts (game saves, database savepoints persisted to disk), the memento must be serializable. This introduces several considerations:

- **Versioning.** The memento's internal format may change as the Originator evolves. You need a migration strategy for old mementos (version fields, backward-compatible deserialization, or explicit format converters).
- **Security.** A serialized memento may contain sensitive data (user input, credentials, session tokens). Ensure mementos are encrypted or stored securely if they leave the process boundary.
- **Object references.** If the Originator's state includes references to other objects, naive serialization can pull in a much larger object graph than intended. Consider what should be serialized by value versus by reference (e.g. an ID that can be used to look up the referenced object on restore).

### Relationship to other patterns

| Pattern | Relationship |
|---|---|
| **Command** | Often used together. Commands represent *operations* and may store the delta needed for undo. Mementos represent *state snapshots*. Some systems use Commands for individual undo steps and Mementos for periodic full checkpoints. |
| **Iterator** | An Iterator over a collection can use Memento to save and restore its traversal position, allowing clients to bookmark a position and return to it later. |
| **Prototype** | Both involve copying state. Prototype clones an entire object to create an independent copy; Memento captures state for later restoration of the *same* object. If the Originator's state is essentially the whole object, the two patterns converge. |
| **Serialization / Serializer** | Serialization is the mechanism often used to implement mementos that need to be persisted. The Memento pattern provides the design structure; serialization provides the infrastructure. |
