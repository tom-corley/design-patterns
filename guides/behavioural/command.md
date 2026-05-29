# Command Pattern

**Classification:** Behavioural (GoF)

---

## 1. Core Idea

The Command pattern encapsulates a request as a standalone object, containing all the
information needed to perform an action (or undo it) at a later time.

### The Problem

In many systems, the object that *triggers* an operation is
tightly coupled to the object that *performs* it. A toolbar button directly calls
`document.save()`, a menu item directly calls `editor.paste()`, and so on. This coupling
makes it difficult to:

- Queue, log, or schedule operations for later execution.
- Support undo and redo.
- Compose simple operations into higher-level macros.
- Parameterise objects with different actions at runtime.

### The Key Insight

If you turn each operation into an object with a common interface
(typically a single `execute()` method), the invoker no longer needs to know *what* the
operation does or *who* carries it out. It only knows that it holds a command and can
execute it. This decouples the "when and how to trigger" from the "what to do."

The pattern is sometimes described as "callbacks done right" -- it gives you the
flexibility of a callback or closure, but with a formal structure that supports undo,
serialisation, queuing, and composition.


---


## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Command** | Declares the interface (usually `execute()` and optionally `undo()`). |
| **ConcreteCommand** | Implements `execute()` by invoking operations on the Receiver. Stores any state needed to carry out (and reverse) the action. |
| **Invoker** | Holds a command and triggers it. Does not know the concrete type of the command or the receiver -- only the `Command` interface. |
| **Receiver** | The object that actually performs the work. The concrete command delegates to it. |
| **Client** | Creates the concrete command, binds it to a receiver, and hands it to the invoker. |

### How they relate

```
  Client
    |
    | creates ConcreteCommand(receiver)
    | passes it to Invoker
    v
  Invoker -----> Command (interface)
                    ^
                    |
              ConcreteCommand ----> Receiver
              (stores state)        (does the real work)
```

1. The **Client** instantiates a `ConcreteCommand` and configures it with a `Receiver`.
2. The **Client** passes the command to an **Invoker** (e.g. a button, a menu, a scheduler).
3. When triggered, the **Invoker** calls `command.execute()`.
4. The **ConcreteCommand** delegates to the **Receiver** to carry out the actual logic.
5. For undo support, the **ConcreteCommand** stores enough state in `execute()` to
   reverse the operation in `undo()`.

The invoker is completely decoupled from the receiver. You can swap commands at runtime,
stack them for undo/redo, serialise them for replay, or batch them into composite macros --
all without modifying the invoker or the receiver.


---


## 3. Use Cases

### Undo / Redo

The classic motivation. Each user action is wrapped in a command that knows how to reverse
itself. An undo stack holds executed commands; popping and calling `undo()` reverses the
last action. A redo stack holds undone commands.

**Examples:** text editors, drawing applications, spreadsheet operations, form builders.

### Task Queues and Deferred Execution

Commands can be placed in a queue and executed later, potentially by a different thread
or process. Because the command is a self-contained object, it carries everything it needs
to execute.

**Examples:** job schedulers, print spoolers, background-task frameworks, message brokers.

### Macro Recording and Playback

A sequence of commands can be stored in a list and replayed. This is the basis of macro
recording in editors and automation tools.

**Examples:** IDE macro recorders, game replay systems, test-automation harnesses.

### Transaction Logging and Crash Recovery

If each command is serialised to a log before execution, the system can replay the log
after a crash to reconstruct state. Combined with undo, this also supports rollback.

**Examples:** database write-ahead logs, event-sourcing architectures, financial audit trails.

### GUI Decoupling

Menus, toolbars, keyboard shortcuts, and context menus can all trigger the same command
object. The UI layer only depends on the `Command` interface, not on the business logic.

**Examples:** virtually every desktop GUI framework (Swing Actions, WPF ICommand).

### Remote Execution

Commands can be serialised and sent across a network for execution on a remote machine.

**Examples:** distributed task execution, RPC frameworks, CI/CD pipeline steps.


---


## 4. Example in Java -- Text Editor with Undo/Redo

This example models a simple text editor where each editing action (insert text, delete
text) is a command. The editor maintains undo and redo stacks.

### Command interface

```java
public interface Command {
    void execute();
    void undo();
}
```

### Receiver -- the Document

The `Document` is the receiver. It holds the text content and provides primitive
operations.

```java
public class Document {

    private final StringBuilder content = new StringBuilder();

    public void insertAt(int position, String text) {
        content.insert(position, text);
    }

    public void deleteRange(int start, int length) {
        content.delete(start, start + length);
    }

    public String getContent() {
        return content.toString();
    }

    public int length() {
        return content.length();
    }

    @Override
    public String toString() {
        return content.toString();
    }
}
```

### Concrete commands

```java
public class InsertTextCommand implements Command {

    private final Document document;
    private final int position;
    private final String text;

    public InsertTextCommand(Document document, int position, String text) {
        this.document = document;
        this.position = position;
        this.text = text;
    }

    @Override
    public void execute() {
        document.insertAt(position, text);
    }

    @Override
    public void undo() {
        document.deleteRange(position, text.length());
    }
}
```

```java
public class DeleteTextCommand implements Command {

    private final Document document;
    private final int position;
    private final int length;
    private String deletedText; // captured on execute for undo

    public DeleteTextCommand(Document document, int position, int length) {
        this.document = document;
        this.position = position;
        this.length = length;
    }

    @Override
    public void execute() {
        // Capture what we are about to delete so we can restore it later.
        deletedText = document.getContent().substring(position, position + length);
        document.deleteRange(position, length);
    }

    @Override
    public void undo() {
        document.insertAt(position, deletedText);
    }
}
```

### Macro command (composite)

A macro is itself a command that holds a list of sub-commands.

```java
import java.util.ArrayList;
import java.util.List;

public class MacroCommand implements Command {

    private final List<Command> commands = new ArrayList<>();

    public void addCommand(Command command) {
        commands.add(command);
    }

    @Override
    public void execute() {
        for (Command command : commands) {
            command.execute();
        }
    }

    @Override
    public void undo() {
        // Undo in reverse order.
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }
}
```

### Invoker -- the Editor

The `Editor` acts as the invoker. It executes commands and manages the undo/redo stacks.

```java
import java.util.ArrayDeque;
import java.util.Deque;

public class Editor {

    private final Document document;
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public Editor(Document document) {
        this.document = document;
    }

    /** Execute a command and push it onto the undo stack. */
    public void executeCommand(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // new action invalidates the redo history
    }

    /** Undo the most recent command. */
    public void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("[Nothing to undo]");
            return;
        }
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
    }

    /** Redo the most recently undone command. */
    public void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("[Nothing to redo]");
            return;
        }
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
    }

    public String getDocumentContent() {
        return document.getContent();
    }
}
```

### Client -- wiring it all together

```java
public class TextEditorDemo {

    public static void main(String[] args) {

        Document document = new Document();
        Editor editor = new Editor(document);

        // --- Basic insert and delete ---

        editor.executeCommand(new InsertTextCommand(document, 0, "Hello, World!"));
        System.out.println("After insert:  \"" + editor.getDocumentContent() + "\"");
        // => "Hello, World!"

        editor.executeCommand(new DeleteTextCommand(document, 5, 8));
        System.out.println("After delete:  \"" + editor.getDocumentContent() + "\"");
        // => "Hello"

        // --- Undo / Redo ---

        editor.undo();
        System.out.println("After undo:    \"" + editor.getDocumentContent() + "\"");
        // => "Hello, World!"

        editor.undo();
        System.out.println("After undo:    \"" + editor.getDocumentContent() + "\"");
        // => ""

        editor.redo();
        System.out.println("After redo:    \"" + editor.getDocumentContent() + "\"");
        // => "Hello, World!"

        // --- Macro command ---

        MacroCommand macro = new MacroCommand();
        macro.addCommand(new DeleteTextCommand(document, 0, 5));
        macro.addCommand(new InsertTextCommand(document, 0, "Goodbye"));

        editor.executeCommand(macro);
        System.out.println("After macro:   \"" + editor.getDocumentContent() + "\"");
        // => "Goodbye, World!"

        editor.undo();
        System.out.println("After undo:    \"" + editor.getDocumentContent() + "\"");
        // => "Hello, World!"
    }
}
```

### Expected output

```
After insert:  "Hello, World!"
After delete:  "Hello"
After undo:    "Hello, World!"
After undo:    ""
After redo:    "Hello, World!"
After macro:   "Goodbye, World!"
After undo:    "Hello, World!"
```


---


## 5. Tradeoffs and Limitations

### When to use the Command pattern

- **You need undo/redo.** This is the pattern's strongest use case. If actions must be
  reversible, commands give you a clean, uniform mechanism.
- **You need to queue, schedule, or log operations.** Commands are self-contained and can
  be stored, serialised, and replayed.
- **You want to decouple the invoker from the receiver.** The invoker depends only on the
  `Command` interface, so you can swap actions at runtime (e.g. reassigning keyboard
  shortcuts).
- **You need macro/composite operations.** A `MacroCommand` that holds a list of
  sub-commands is trivial to implement and composes naturally.
- **You want to support transaction-style rollback.** Execute a batch of commands; if one
  fails, undo the rest in reverse order.

### When not to use it

- **The operation is trivially simple and will never need undo, queuing, or logging.**
  Wrapping a one-line method call in a command class is pure overhead if you will never
  exploit the pattern's benefits.
- **You already have a simpler abstraction that fits.** A `Runnable`, a lambda, or a
  method reference may be all you need if you only want deferred execution without undo.
- **The state required for undo is prohibitively large.** If reversing an operation
  requires snapshotting a large data structure, the Memento pattern or event-sourcing may
  be more appropriate.

### Class proliferation

The main practical downside is the number of small classes. Every distinct action becomes
its own `ConcreteCommand`. In a complex application (think a full-featured drawing tool),
you can end up with dozens of command classes. Strategies to manage this:

- **Lambdas and functional interfaces.** In modern Java (8+), if a command is stateless
  and does not need `undo()`, you can use a `Runnable` or a custom functional interface
  instead of a concrete class. This eliminates many trivial command files.
- **Parameterised commands.** Instead of one class per action, create a general-purpose
  command that takes a lambda for execute and a lambda for undo:

```java
public class LambdaCommand implements Command {

    private final Runnable executeAction;
    private final Runnable undoAction;

    public LambdaCommand(Runnable executeAction, Runnable undoAction) {
        this.executeAction = executeAction;
        this.undoAction = undoAction;
    }

    @Override
    public void execute() {
        executeAction.run();
    }

    @Override
    public void undo() {
        undoAction.run();
    }
}
```

  This trades type-safety and clarity for fewer classes. Use it for ad-hoc or one-off
  commands, not as a wholesale replacement for named command classes in a complex domain.

- **Inner or anonymous classes.** For commands used in only one place, a local or anonymous
  class keeps the command definition close to its usage without polluting the top-level
  package.

### Command vs. simple lambdas in modern Java

Java 8 gave us lambdas and functional interfaces, which overlap with the simplest form of
the Command pattern (a single `execute()` with no undo). The question is: when do you
still need the full Command pattern?

| Concern | Lambda / `Runnable` | Full Command object |
|---|---|---|
| Deferred execution | Yes | Yes |
| Undo/redo | No (no state, no `undo()`) | Yes |
| Serialisation / logging | Awkward (lambdas are not easily serialisable) | Yes (commands are regular objects) |
| Macro composition | Possible but ad-hoc | Natural via `MacroCommand` |
| Readability at scale | Degrades with complex logic | Each command is a named, testable class |

**Rule of thumb:** Use lambdas for fire-and-forget callbacks. Use the Command pattern when
you need undo, logging, queuing, serialisation, or when the operation carries meaningful
state.

### Related patterns

- **Memento** is often used alongside Command to snapshot receiver state for undo when the
  command cannot easily reverse its own effect.
- **Composite** is used to build macro commands (a command that contains other commands).
- **Strategy** also encapsulates behaviour in objects, but Strategy typically selects *one*
  algorithm for a context, whereas Command encapsulates *individual requests* that may be
  stored, queued, or undone.
- **Observer** can be combined with Command: observers react to events, and the event
  payload can be a command object.
