# Memento

- The idea of this pattern is to take a snapshot of an object without leaking its internals to client code, which is better for encapsulation and for security
- Three roles: the originator (the object being snapshotted), the memento (the snapshot itself, immutable), and the caretaker (stores mementos but cannot peek inside them)
- A static nested class is a natural fit for the memento, it can access the originator's private fields but doesn't hold a reference to a live instance, and a private constructor means only the originator can create snapshots
- The canonical use case is undo/redo with two stacks, important to save the current state before restoring otherwise you lose the state you are undoing from
- The main cost is memory, every snapshot is a full copy, can be mitigated with stack limits or incremental diffs
- Be careful with deep vs shallow copies, if the originator holds references to mutable objects the memento must deep copy them
- Differs from command pattern undo in that command stores the inverse operation, memento stores the full state 