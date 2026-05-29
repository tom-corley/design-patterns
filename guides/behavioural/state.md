# State Pattern

**Classification:** Behavioural (GoF)

---

## 1. Core Idea

The State pattern allows an object to **change its behaviour when its internal state changes**, making it appear as if the object has changed its class. It replaces sprawling conditional logic (`if`/`else`, `switch` on an enum) with polymorphism: each state becomes its own class, and behaviour that varies by state is delegated to whichever state object is currently active.

### The Problem

Consider an e-commerce order. An order passes through several states -- Pending, Paid, Shipped, Delivered, Cancelled -- and the operations you can perform depend on which state it is in. You can pay a Pending order, but not a Delivered one. You can ship a Paid order, but not a Cancelled one. You can cancel a Pending or Paid order, but not one that has already shipped.

The naive approach is to check the current state in every method:

```java
public void pay() {
    if (state == PENDING) {
        // process payment...
        state = PAID;
    } else if (state == PAID) {
        throw new IllegalStateException("Already paid");
    } else if (state == SHIPPED) {
        throw new IllegalStateException("Cannot pay a shipped order");
    }
    // ... and so on for every state
}
```

Every operation contains a switch over every possible state. As you add more states or more operations, the conditionals multiply. The class accumulates hundreds of lines of interleaved conditional logic that is difficult to read, easy to break, and painful to extend.

### The Key Insight

Each state already has a coherent identity -- it knows which operations are valid, what those operations do, and which state to transition to next. If you extract each state into its own class behind a common interface, the context object can simply delegate to whatever state object it currently holds. Adding a new state means adding a new class that implements the interface; existing states are untouched. The conditional logic vanishes, replaced by polymorphic dispatch.

This is the Open/Closed Principle in action: the system is open for extension (new states) and closed for modification (existing state classes do not change).

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Context** | The object whose behaviour changes with state. Holds a reference to the current `State` object. Delegates state-dependent operations to it. Exposes a method (often package-private or internal) that allows the state to trigger a transition. |
| **State** | An interface (or abstract class) declaring the operations that vary by state. Each method typically receives the `Context` as a parameter so the state can trigger transitions. |
| **ConcreteState** | One class per distinct state. Implements the `State` interface with behaviour specific to that state, including deciding which state to transition to when an operation succeeds. |

### How Transitions Work

```
  Context
  ┌──────────────────────────┐
  │  - currentState: State   │
  │                          │
  │  + pay()                 │──── delegates ────>  State (interface)
  │  + ship()                │                        │
  │  + deliver()             │                  ┌─────┼─────────┐
  │  + cancel()              │                  │     │         │
  │                          │              Pending  Paid    Shipped ...
  │  + setState(State)       │
  └──────────────────────────┘
```

1. The client calls an operation on the `Context` (e.g. `order.pay()`).
2. The `Context` forwards the call to its current `State` object: `currentState.pay(this)`.
3. The `ConcreteState` executes the behaviour appropriate for that state.
4. If the operation triggers a transition, the `ConcreteState` calls `context.setState(new NextState())` to install the successor state.
5. The next call to any operation on the `Context` will now be handled by the new state.

### Who Owns the Transition Logic?

There are two common approaches:

- **State-driven transitions (most common).** Each `ConcreteState` decides which state to transition to. This keeps the transition logic close to the behaviour it depends on, but it means concrete states know about each other.
- **Context-driven transitions.** The `Context` inspects the result of the delegated call and decides the next state. This centralises the transition table but can re-introduce conditional logic in the context.

In practice, state-driven transitions are more natural for the State pattern. If you find yourself wanting a centralised transition table, consider whether a finite-state machine framework is a better fit.

---

## 3. Use Cases

### Order Lifecycle

An order moves through Pending, Paid, Shipped, Delivered, and Cancelled. Each state defines which operations are valid (pay, ship, deliver, cancel, refund) and what the next state is. Invalid operations in a given state throw or return an error. This is the example we implement below.

### Document Approval Workflow

A document can be in Draft, UnderReview, Approved, or Rejected state. Authors can edit drafts but not approved documents. Reviewers can approve or reject documents under review. Each transition may have side effects (sending notifications, logging an audit trail). The State pattern keeps each stage's rules self-contained.

### TCP Connection

The classic GoF example. A TCP connection object can be in Established, Listening, or Closed state. Operations like `open()`, `close()`, and `acknowledge()` behave differently depending on the connection's current state. Each state class encapsulates the protocol behaviour for that phase of the connection lifecycle.

### Vending Machine

A vending machine cycles through states: Idle, CoinInserted, Dispensing, OutOfStock. Inserting a coin in the Idle state transitions to CoinInserted. Pressing the dispense button in CoinInserted checks stock and either dispenses (transitioning to Dispensing, then back to Idle) or refuses. Each state handles the same set of inputs (insert coin, press button, return change) with different outcomes.

### Media Player

A media player has Playing, Paused, and Stopped states. Pressing "play" while stopped starts playback from the beginning; pressing "play" while paused resumes from the current position. Pressing "stop" while already stopped is a no-op. The State pattern avoids a tangle of conditionals in every button handler.

---

## 4. Example in Java

This example models an **order lifecycle** with five states: Pending, Paid, Shipped, Delivered, and Cancelled. The `Order` (context) delegates operations to the current state. Each state class decides what is allowed and which state comes next.

### State Interface -- `OrderState`

```java
/**
 * The State interface. Declares every operation that varies by state.
 * Default implementations throw IllegalStateException so that concrete
 * states only need to override the operations they support.
 */
public interface OrderState {

    default void pay(Order order) {
        throw new IllegalStateException(
                "Cannot pay an order in " + name() + " state");
    }

    default void ship(Order order) {
        throw new IllegalStateException(
                "Cannot ship an order in " + name() + " state");
    }

    default void deliver(Order order) {
        throw new IllegalStateException(
                "Cannot deliver an order in " + name() + " state");
    }

    default void cancel(Order order) {
        throw new IllegalStateException(
                "Cannot cancel an order in " + name() + " state");
    }

    /**
     * Human-readable name of this state, used in logging and error messages.
     */
    String name();
}
```

Using default methods that throw means each concrete state only overrides the operations that are valid in that state. Any operation not overridden produces a clear error message. This avoids large amounts of boilerplate "unsupported" methods in every state class.

### Concrete States

```java
public class PendingState implements OrderState {

    @Override
    public void pay(Order order) {
        System.out.println("Processing payment for order " + order.getId() + "...");
        System.out.println("Payment accepted. Order is now Paid.");
        order.setState(new PaidState());
    }

    @Override
    public void cancel(Order order) {
        System.out.println("Order " + order.getId() + " cancelled (was pending).");
        order.setState(new CancelledState());
    }

    @Override
    public String name() {
        return "Pending";
    }
}
```

```java
public class PaidState implements OrderState {

    @Override
    public void ship(Order order) {
        System.out.println("Order " + order.getId() + " handed to carrier. Shipping in progress.");
        order.setState(new ShippedState());
    }

    @Override
    public void cancel(Order order) {
        System.out.println("Order " + order.getId()
                + " cancelled. Refund of payment will be issued.");
        order.setState(new CancelledState());
    }

    @Override
    public String name() {
        return "Paid";
    }
}
```

```java
public class ShippedState implements OrderState {

    @Override
    public void deliver(Order order) {
        System.out.println("Order " + order.getId()
                + " delivered to customer. Order complete.");
        order.setState(new DeliveredState());
    }

    @Override
    public String name() {
        return "Shipped";
    }

    // Note: cancel() and pay() are not overridden, so they throw.
    // A shipped order cannot be cancelled through this simple model.
}
```

```java
public class DeliveredState implements OrderState {

    @Override
    public String name() {
        return "Delivered";
    }

    // A delivered order is a terminal state. No operations are valid.
    // All methods inherit the default IllegalStateException behaviour.
}
```

```java
public class CancelledState implements OrderState {

    @Override
    public String name() {
        return "Cancelled";
    }

    // A cancelled order is a terminal state. No operations are valid.
}
```

### Context -- `Order`

```java
/**
 * The Context. Holds the current state and delegates operations to it.
 */
public class Order {

    private final String id;
    private OrderState state;

    public Order(String id) {
        this.id = id;
        this.state = new PendingState();
        System.out.println("Order " + id + " created in Pending state.");
    }

    public void pay() {
        state.pay(this);
    }

    public void ship() {
        state.ship(this);
    }

    public void deliver() {
        state.deliver(this);
    }

    public void cancel() {
        state.cancel(this);
    }

    /**
     * Called by state objects to transition the order to a new state.
     */
    void setState(OrderState newState) {
        System.out.println("  [" + id + "] State transition: "
                + this.state.name() + " -> " + newState.name());
        this.state = newState;
    }

    public String getId() {
        return id;
    }

    public String getStateName() {
        return state.name();
    }
}
```

Notice that `setState` is package-private. State objects (which live in the same package) can call it to trigger transitions, but external clients cannot bypass the state machine by forcing a state change directly.

### Client Code

```java
public class Main {

    public static void main(String[] args) {

        // --- Happy path: Pending -> Paid -> Shipped -> Delivered ---
        System.out.println("=== Order ORD-001: happy path ===");
        Order order1 = new Order("ORD-001");
        order1.pay();
        order1.ship();
        order1.deliver();
        System.out.println("Final state: " + order1.getStateName());
        System.out.println();

        // --- Cancellation after payment ---
        System.out.println("=== Order ORD-002: cancel after payment ===");
        Order order2 = new Order("ORD-002");
        order2.pay();
        order2.cancel();
        System.out.println("Final state: " + order2.getStateName());
        System.out.println();

        // --- Invalid operation: try to ship a pending order ---
        System.out.println("=== Order ORD-003: invalid operation ===");
        Order order3 = new Order("ORD-003");
        try {
            order3.ship(); // should fail -- order has not been paid
        } catch (IllegalStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        System.out.println("Final state: " + order3.getStateName());
        System.out.println();

        // --- Invalid operation: try to cancel a delivered order ---
        System.out.println("=== Order ORD-004: cancel after delivery ===");
        Order order4 = new Order("ORD-004");
        order4.pay();
        order4.ship();
        order4.deliver();
        try {
            order4.cancel(); // should fail -- already delivered
        } catch (IllegalStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }
        System.out.println("Final state: " + order4.getStateName());
    }
}
```

### Expected Output

```
=== Order ORD-001: happy path ===
Order ORD-001 created in Pending state.
Processing payment for order ORD-001...
Payment accepted. Order is now Paid.
  [ORD-001] State transition: Pending -> Paid
Order ORD-001 handed to carrier. Shipping in progress.
  [ORD-001] State transition: Paid -> Shipped
Order ORD-001 delivered to customer. Order complete.
  [ORD-001] State transition: Shipped -> Delivered
Final state: Delivered

=== Order ORD-002: cancel after payment ===
Order ORD-002 created in Pending state.
Processing payment for order ORD-002...
Payment accepted. Order is now Paid.
  [ORD-002] State transition: Pending -> Paid
Order ORD-002 cancelled. Refund of payment will be issued.
  [ORD-002] State transition: Paid -> Cancelled
Final state: Cancelled

=== Order ORD-003: invalid operation ===
Order ORD-003 created in Pending state.
Caught: Cannot ship an order in Pending state
Final state: Pending

=== Order ORD-004: cancel after delivery ===
Order ORD-004 created in Pending state.
Processing payment for order ORD-004...
Payment accepted. Order is now Paid.
  [ORD-004] State transition: Pending -> Paid
Order ORD-004 handed to carrier. Shipping in progress.
  [ORD-004] State transition: Paid -> Shipped
Order ORD-004 delivered to customer. Order complete.
  [ORD-004] State transition: Shipped -> Delivered
Caught: Cannot cancel an order in Delivered state
Final state: Delivered
```

### Why This Works

Without the State pattern, the `Order` class would contain a `switch` or `if/else` chain in `pay()`, `ship()`, `deliver()`, and `cancel()` -- four methods, each with five branches, totalling twenty conditional branches that grow in lockstep with every new state or operation. Adding a "Refunded" state would mean editing every method.

With the State pattern, adding a "Refunded" state means writing one new `RefundedState` class and updating the relevant existing state (e.g. `DeliveredState` gains an override for a `refund()` method that transitions to `RefundedState`). No other state classes are modified.

---

## 5. Tradeoffs and Limitations

### When to Use It

- **An object has distinct behavioural modes.** If the behaviour of an object changes substantially depending on its internal state, and you find yourself writing parallel `if/else` or `switch` blocks in multiple methods, the State pattern is a strong candidate.
- **State transitions are well-defined.** The pattern works best when the set of states and valid transitions between them form a clear graph. If the rules are fuzzy or highly dynamic, a data-driven state machine (transition table) may be more appropriate than a class hierarchy.
- **You anticipate adding new states.** The pattern's primary payoff is extensibility. If the state space is small and unlikely to grow (e.g. on/off), the overhead of separate classes is not justified.

### When Not to Use It

- **Trivial state logic.** If you have two or three states and one or two operations, an enum and a switch statement are simpler, more readable, and easier to follow. The State pattern has a fixed structural cost (one interface + N classes) that only pays off when the alternative -- conditional logic scattered across multiple methods -- is genuinely painful.
- **State is derived, not stored.** If "state" is computed from other fields (e.g. an order is "overdue" if its due date has passed), there is no real state to encapsulate in an object. A method or a computed property is the right tool.
- **Performance-critical inner loops.** Each state method call involves a virtual dispatch through the state interface. In the vast majority of applications this overhead is negligible, but in tight inner loops (game engines, real-time simulations ticking millions of entities per frame), the indirection and object allocation can matter.

### State Explosion

The State pattern trades conditional complexity for class complexity. If you have 15 states and 10 operations, you need 15 classes, each potentially implementing 10 methods. This is not necessarily worse than 10 methods with 15-branch switches, but it is a different kind of complexity. Keep an eye on whether the number of states is growing beyond what is manageable as separate classes. If it is, consider a table-driven state machine where transitions are defined as data rather than code.

### Who Owns Transitions: The Coupling Trade-Off

When concrete states drive transitions (calling `context.setState(new NextState())`), each state knows about its successor states. This creates coupling between state classes. In the order example, `PendingState` references `PaidState` and `CancelledState`. This coupling is usually acceptable because the states are part of a cohesive state machine and change together. However, if you need the same state classes to participate in different state machines with different transition graphs, you will need to externalise the transition logic -- either into the context or into a separate transition table.

### State vs Strategy

The State and Strategy patterns are structurally identical: a context holds a reference to an interface and delegates to it. The difference is entirely in intent and lifecycle:

| | State | Strategy |
|---|---|---|
| **Purpose** | Model an object whose behaviour changes over time as it moves through a lifecycle of distinct states | Allow a client to choose among interchangeable algorithms at a point in time |
| **Who triggers the swap** | The state objects themselves, as a side effect of an operation (automatic, internal) | The client or configuration code, based on requirements (explicit, external) |
| **Awareness between implementations** | Concrete states typically know about each other because they define transitions | Concrete strategies are independent and unaware of each other |
| **Number of swaps** | Many, over the lifetime of the context, as the object progresses through its lifecycle | Usually once (at construction or configuration time), or infrequently |

A useful heuristic: if the object's "mode" changes as a result of its own operations and the set of valid operations varies by mode, you have State. If the client picks an algorithm up front and the set of operations does not change, you have Strategy.
