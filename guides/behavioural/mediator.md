# Mediator Pattern

**Classification:** Behavioural (GoF)

**Also known as:** Controller, Intermediary

---

## 1. Core Idea

The Mediator pattern defines an object that encapsulates how a set of objects interact. Instead of each object holding direct references to every other object it needs to talk to, all communication is routed through a single mediator. The colleagues know only about the mediator; the mediator knows about all the colleagues. This converts an n-to-n mesh of dependencies into a star topology centered on the mediator.

### The Problem

Consider a complex dialog window with a form. A checkbox enables or disables a text field. Selecting a value in a dropdown changes which radio buttons are visible. A button should only be enabled when certain fields are non-empty and a checkbox is ticked. Each control needs to react to changes in several other controls.

If every control holds direct references to the controls it depends on, you end up with a dense web of bidirectional couplings. Adding a new control means modifying every existing control it interacts with. Testing any single control requires instantiating all its partners. The form logic is smeared across a dozen small classes, making it nearly impossible to understand the overall coordination policy by reading any one of them.

The same problem appears whenever you have a group of objects that must collaborate according to rules that are complex, changeable, or both -- chat participants, aircraft near an airport, microservices behind an orchestrator, or game entities reacting to each other.

### The Key Insight

Extract the interaction logic out of the individual objects and concentrate it in a dedicated mediator object. Each participant (a "colleague") holds a single reference to the mediator and sends it notifications when something interesting happens. The mediator contains the rules that determine how to react -- which other colleagues to notify, what state to change, what to enable or disable. The colleagues become simple, self-contained, and reusable; the coordination complexity lives in exactly one place.

This is a tradeoff: you exchange many small couplings for one large coupling point. The system becomes easier to understand at the component level (each colleague is trivial) at the cost of the mediator itself becoming a potentially complex class. The tradeoff pays off when the interaction rules are complex enough that scattering them across participants makes the system harder to reason about than centralising them.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Mediator** | Defines the interface through which colleagues communicate. Typically a single method like `notify(sender, event)`. |
| **ConcreteMediator** | Implements the coordination logic. Holds references to all concrete colleagues and reacts to their notifications by invoking methods on other colleagues. |
| **Colleague** | Base class (or interface) for objects that participate in the mediated interaction. Each colleague holds a reference to its mediator. |
| **ConcreteColleague** | A specific participant. Knows nothing about other colleagues. When its state changes or it needs to trigger a cross-cutting action, it notifies the mediator. |

### Relationships

```
       +---------------------+                +---------------------+
       |     Mediator         |                |     Colleague       |
       |---------------------|                |---------------------|
       | + notify(sender,    |<---- knows ----|  - mediator         |
       |         event)      |                | + setMediator(m)    |
       +---------------------+                +---------------------+
               ^                                       ^
               |                                       |
   +-----------------------+          +----------------------------+
   |   ConcreteMediator    |          |    ConcreteColleagueA      |
   |-----------------------|          |----------------------------|
   | - colleagueA          |          | + doSomething()            |
   | - colleagueB          |          | + onChange()               |
   | - colleagueC          |          |   { mediator.notify(this,  |
   | + notify(sender,      |          |       "changed"); }        |
   |         event)        |          +----------------------------+
   |   { if sender == A    |
   |       then B.react()  |          +----------------------------+
   |     if sender == B    |          |    ConcreteColleagueB      |
   |       then C.react()  |          |----------------------------|
   |     ... }             |          | + react()                  |
   +-----------------------+          +----------------------------+
```

**Communication flow:**

1. ConcreteColleagueA detects a change in its own state (user input, timer, etc.).
2. It calls `mediator.notify(this, "eventName")`.
3. The ConcreteMediator's `notify` method inspects the sender and event, then calls the appropriate methods on other colleagues (B, C, etc.).
4. The other colleagues react without knowing who triggered the action or why.

Colleagues never call each other directly. All cross-object coordination passes through the mediator.

---

## 3. Use Cases

**Chat rooms.** Users send messages to a chat room (the mediator), not directly to other users. The chat room decides how to distribute the message -- to all participants, to a specific channel, or filtered by some rule. Each user object only knows how to send to and receive from the room.

**Air traffic control.** Aircraft approaching an airport do not negotiate landing order among themselves. The control tower (mediator) receives position and intent reports from each aircraft and issues clearances. No pilot needs to know the state of every other aircraft; the tower centralises that awareness.

**UI dialog coordination.** A form with interdependent controls -- enabling a checkbox reveals a panel, selecting a value in a list repopulates a dropdown, filling all required fields enables a submit button. A dialog mediator receives change events from each control and updates the others, keeping the individual controls unaware of each other.

**Event buses and message brokers.** An in-process event bus is a generalised mediator: components publish events to the bus, and the bus routes them to registered subscribers. The publishers and subscribers are decoupled; only the bus knows the wiring. Message brokers like RabbitMQ and Kafka are the same idea at a distributed scale.

**Game entity coordination.** In a game, a collision manager can mediate between entities. When a projectile hits a shield, neither object needs a direct reference to the other. Both report to the collision mediator, which resolves the interaction (reduce shield health, destroy projectile, play sound effect).

---

## 4. Example in Java

This example models a chat room. Users send messages through a `ChatRoom` mediator, which distributes them to all other participants. The example also shows private messaging routed through the same mediator.

### Mediator interface

```java
/**
 * Mediator interface. Defines how colleagues (users) communicate through
 * the chat room.
 */
public interface ChatMediator {

    /**
     * Register a user with this chat room.
     */
    void addUser(User user);

    /**
     * Broadcast a message from the sender to all other registered users.
     */
    void sendMessage(String message, User sender);

    /**
     * Send a private message from one user to another, routed through
     * the mediator so neither user holds a direct reference to the other.
     */
    void sendDirectMessage(String message, User sender, String recipientName);
}
```

### Colleague base class

```java
/**
 * Colleague base class. Each user holds a reference to the mediator and
 * communicates exclusively through it.
 */
public abstract class User {

    protected final ChatMediator mediator;
    protected final String name;

    public User(ChatMediator mediator, String name) {
        this.mediator = mediator;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Send a message to the room (broadcast).
     */
    public abstract void send(String message);

    /**
     * Send a private message to a named recipient.
     */
    public abstract void sendDirect(String message, String recipientName);

    /**
     * Called by the mediator when this user should receive a message.
     */
    public abstract void receive(String message, String fromUser);
}
```

### Concrete colleague

```java
/**
 * A standard chat participant. All interaction with other users goes
 * through the mediator.
 */
public class ChatUser extends User {

    public ChatUser(ChatMediator mediator, String name) {
        super(mediator, name);
    }

    @Override
    public void send(String message) {
        System.out.println(name + " sends: " + message);
        mediator.sendMessage(message, this);
    }

    @Override
    public void sendDirect(String message, String recipientName) {
        System.out.println(name + " sends DM to " + recipientName + ": " + message);
        mediator.sendDirectMessage(message, this, recipientName);
    }

    @Override
    public void receive(String message, String fromUser) {
        System.out.println("[" + name + " received] " + fromUser + ": " + message);
    }
}
```

### Concrete mediator

```java
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete mediator. Manages user registration and message routing.
 *
 * This is where the coordination logic lives. Individual users know nothing
 * about each other; the ChatRoom decides who gets which messages.
 */
public class ChatRoom implements ChatMediator {

    private final String roomName;
    private final List<User> users = new ArrayList<>();

    public ChatRoom(String roomName) {
        this.roomName = roomName;
    }

    @Override
    public void addUser(User user) {
        users.add(user);
        System.out.println("[" + roomName + "] " + user.getName() + " joined the room.");
    }

    @Override
    public void sendMessage(String message, User sender) {
        for (User user : users) {
            // Do not echo the message back to the sender.
            if (user != sender) {
                user.receive(message, sender.getName());
            }
        }
    }

    @Override
    public void sendDirectMessage(String message, User sender, String recipientName) {
        for (User user : users) {
            if (user.getName().equals(recipientName)) {
                user.receive("[DM] " + message, sender.getName());
                return;
            }
        }
        System.out.println("[" + roomName + "] User '" + recipientName + "' not found.");
    }
}
```

### Client code

```java
public class Main {

    public static void main(String[] args) {
        // Create the mediator.
        ChatMediator room = new ChatRoom("general");

        // Create colleagues. Each only knows about the mediator.
        User alice = new ChatUser(room, "Alice");
        User bob   = new ChatUser(room, "Bob");
        User carol = new ChatUser(room, "Carol");

        // Register colleagues with the mediator.
        room.addUser(alice);
        room.addUser(bob);
        room.addUser(carol);

        System.out.println();

        // Alice broadcasts -- Bob and Carol receive, Alice does not.
        alice.send("Hey everyone, standup in 5 minutes.");

        System.out.println();

        // Bob sends a direct message to Carol through the mediator.
        bob.sendDirect("Can you review my PR before standup?", "Carol");

        System.out.println();

        // Carol broadcasts.
        carol.send("On my way!");

        System.out.println();

        // Attempting a DM to a non-existent user.
        alice.sendDirect("Hello?", "Dave");
    }
}
```

### Expected output

```
[general] Alice joined the room.
[general] Bob joined the room.
[general] Carol joined the room.

Alice sends: Hey everyone, standup in 5 minutes.
[Bob received] Alice: Hey everyone, standup in 5 minutes.
[Carol received] Alice: Hey everyone, standup in 5 minutes.

Bob sends DM to Carol: Can you review my PR before standup?
[Carol received] Bob: [DM] Can you review my PR before standup?

Carol sends: On my way!
[Alice received] Carol: On my way!
[Bob received] Carol: On my way!

Alice sends DM to Dave: Hello?
[general] User 'Dave' not found.
```

### Why this works

- **Users are decoupled.** Alice has no reference to Bob or Carol. Adding a new user (Dave) requires zero changes to any existing user class or to the `ChatUser` implementation.
- **Routing logic is centralised.** Broadcast rules, DM routing, filtering (e.g. muting a user, applying profanity filters) would all be added to the `ChatRoom` class -- one place to look, one place to test.
- **Colleagues are reusable.** The same `ChatUser` class works with any `ChatMediator` implementation -- a logged chat room, an encrypted chat room, a room that rate-limits messages. The user does not change.
- **Testability.** You can test a `ChatUser` with a mock mediator, verifying it calls `sendMessage` without needing to instantiate a real room or other users. You can test the `ChatRoom` by injecting stub users.

---

## 5. Tradeoffs and Limitations

### When to use it

- You have a set of objects with complex, intertwined communication rules that are difficult to understand when distributed across the individual objects.
- You want to reuse individual components in different contexts without dragging along all the objects they currently interact with.
- The interaction logic is likely to change independently of the individual components (e.g. business rules for form validation change often, but the UI controls themselves are stable).
- You find yourself unable to test a component without setting up a large graph of its collaborators.

### When not to use it

- **The interactions are simple and few.** If object A only talks to object B, introducing a mediator adds a layer of indirection for no real benefit. Direct references are clearer when the relationship graph is small and stable.
- **The communication is purely one-directional and event-driven.** If objects simply need to react to state changes in other objects without complex conditional logic, the Observer pattern is lighter and more appropriate. Mediator shines when the routing logic is conditional, stateful, or involves coordinating multiple colleagues at once.
- **You do not need to swap or reuse the coordination logic.** If the mediator would only ever have one implementation and the colleagues are never used outside this specific context, the pattern's abstraction overhead may not pay for itself.

### The "god object" risk

This is the most important limitation. The mediator centralises all interaction logic, and in a complex system that logic can be substantial. A mediator that coordinates 15 UI controls with dozens of interdependent rules can easily become an unreadable, untestable monster -- the very kind of class the pattern was supposed to prevent.

Mitigations:

- **Decompose the mediator.** Split a large mediator into smaller sub-mediators, each handling a subset of the coordination (e.g. one for form validation, one for layout toggling).
- **Use event-based notification** within the mediator to keep its internal logic declarative rather than a cascade of if-else branches.
- **Keep the mediator focused on routing, not business logic.** If the mediator starts computing domain results rather than just telling colleagues to update, it has taken on too much responsibility.

### Mediator vs Observer

These two patterns are closely related and often confused. The difference is in where the intelligence sits:

| Aspect | Observer | Mediator |
|---|---|---|
| **Topology** | One-to-many: a subject notifies a list of observers. | Many-to-many: colleagues notify the mediator, which routes to other colleagues. |
| **Intelligence** | Distributed. Each observer decides what to do when notified. The subject has no routing logic. | Centralised. The mediator decides who gets notified and what happens. |
| **Coupling** | Observers are coupled to the subject's event interface. They are independent of each other. | Colleagues are coupled only to the mediator interface. The mediator is coupled to all colleagues. |
| **Best for** | Reacting to state changes when the reaction logic is local to each observer. | Coordinating interactions that involve conditional logic, sequencing, or multiple participants at once. |

In practice, a mediator often *uses* the Observer pattern internally -- colleagues fire events, and the mediator subscribes to them. The mediator adds a layer of orchestration logic on top of the raw event stream.

### Alternatives to consider

| Alternative | When to prefer it |
|---|---|
| **Observer** | The interaction is purely reactive (state change triggers independent reactions), with no conditional cross-object coordination. |
| **Facade** | You want to simplify access to a subsystem but do not need bidirectional communication between subsystem components. Facade is one-directional; Mediator is bidirectional. |
| **Command** | The coordination involves undoable, queueable, or loggable operations. Commands can be routed through a mediator, but if the primary concern is operation reification rather than decoupling colleagues, Command is the better fit. |
| **Event Bus / Message Broker** | You need the decoupling benefits of a mediator but at a larger scale (cross-module, cross-service), with features like persistence, replay, and asynchronous delivery. |

### Relationship to other patterns

- **Facade** and Mediator both sit in front of a group of objects, but Facade provides a simplified unidirectional API into a subsystem, while Mediator enables bidirectional communication among peers.
- **Observer** is a building block frequently used inside mediator implementations. The mediator adds conditional routing logic that plain Observer lacks.
- **Command** objects are often the messages passed through a mediator, especially in undo-capable systems.
- **Chain of Responsibility** is an alternative when requests should be handled by one of several potential handlers in sequence, rather than routed by a central coordinator.
