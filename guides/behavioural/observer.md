# Observer Pattern

**Classification:** Behavioural (GoF)

---

## 1. Core Idea

The Observer pattern defines a **one-to-many dependency** between objects so that when one object (the subject) changes state, all its dependents (observers) are notified and updated automatically.

### The Problem

You have an object whose state is interesting to other parts of the system. A stock price changes and three different dashboard widgets need to redraw. A user updates their profile and a notification service, an audit logger, and a cache all need to react. The naive solution is to have the source of truth call each dependent directly, but this creates tight coupling: the subject must know about every consumer, every consumer's interface, and the full list must be updated any time a new consumer appears.

An alternative -- having each interested party poll the subject for changes -- wastes resources and introduces latency. If the polling interval is too long, consumers see stale data. If it is too short, you burn CPU checking for changes that rarely happen.

### The Key Insight

Invert the dependency. Let the subject maintain a list of observers that have **registered** their interest. When the subject's state changes, it iterates through the list and calls a well-known update method on each observer. The subject does not need to know what the observers do with the information -- it only knows they implement a common interface. Observers can be added and removed at runtime without modifying the subject.

This is the publish-subscribe principle at the object level: the subject publishes events, observers subscribe to them, and the coupling between publisher and subscriber is limited to a single interface.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Subject** (Observable) | Maintains a collection of observers. Provides methods to attach, detach, and notify observers. Knows nothing about what observers do -- only that they implement the `Observer` interface. |
| **ConcreteSubject** | Stores state of interest to observers. When its state changes, it calls the inherited `notifyObservers()` method. May provide accessor methods so observers can pull the current state. |
| **Observer** | Defines an update interface for objects that should be notified of changes in a subject. |
| **ConcreteObserver** | Implements the `Observer` interface. Maintains a reference to (or receives data from) a `ConcreteSubject`. Reconciles its own state with the subject's state when `update()` is called. |

### Relationships

```
    Subject                           Observer
    ├─ attach(Observer)               └─ update(...)
    ├─ detach(Observer)                    ^
    ├─ notifyObservers()                   |
    │         |                            |
    │     iterates and calls ──────────────┘
    │
    v
  ConcreteSubject               ConcreteObserverA
    └─ getState()               ConcreteObserverB
    └─ setState(...)            ConcreteObserverC
```

1. The **ConcreteSubject** changes state (or has its state changed externally).
2. It calls `notifyObservers()`, which iterates through the registered observer list.
3. Each **ConcreteObserver** receives the `update()` call and reacts accordingly.

### Push vs Pull Models

There are two common strategies for how observers receive data:

**Push model:** The subject sends detailed change information directly in the `update()` call (e.g. `update(String ticker, double newPrice)`). The observer receives everything it needs without calling back to the subject. This is simpler for the observer, but the subject must anticipate what data each observer needs. If different observers need different data, the push payload tends to bloat.

**Pull model:** The subject sends a minimal notification (often just a reference to itself), and the observer calls back to the subject's getters to retrieve whatever data it needs (e.g. `update(Subject source)` followed by `source.getPrice()`). This keeps the notification interface lean but introduces a tighter coupling between the observer and the concrete subject type.

In practice, a hybrid approach is common: push enough context to let observers decide whether they care (e.g. which property changed), and let them pull the details they need.

---

## 3. Use Cases

### Event Listeners (GUI Frameworks)

The most familiar example. A button (subject) maintains a list of `ActionListener` instances (observers). When the button is clicked, it fires `actionPerformed()` on each listener. Swing, JavaFX, and virtually every GUI toolkit is built on this pattern. The framework component does not know or care what the listener does -- it could update a label, write to a log, or trigger a network request.

### Publish-Subscribe / Message Brokers

The Observer pattern is the in-process ancestor of distributed pub/sub systems. Kafka topics, RabbitMQ exchanges, and Redis pub/sub channels all implement the same principle at a larger scale: publishers emit events, subscribers register interest, and a broker (the subject) routes messages. In-process event buses like Guava's `EventBus` or Spring's `ApplicationEventPublisher` are direct implementations of Observer.

### Reactive Streams

Project Reactor, RxJava, and `java.util.concurrent.Flow` (introduced in Java 9) formalise the Observer pattern with backpressure support. A `Publisher` emits items to `Subscriber` instances that have registered via `subscribe()`. The `onNext()` / `onError()` / `onComplete()` protocol is a structured version of the observer's `update()` method. The reactive model adds flow control that the classic GoF Observer lacks.

### Model-View-Controller (MVC)

MVC uses Observer to keep the view in sync with the model. The model (subject) notifies attached views (observers) when its data changes. Each view re-renders itself based on the new model state. The controller mediates user input but does not carry the notification responsibility -- that flows directly from model to view via the Observer relationship.

### Domain Event Systems

In domain-driven design, aggregate roots may publish domain events (e.g. `OrderPlaced`, `PaymentReceived`). Event handlers (observers) listen for these events and trigger side effects -- sending a confirmation email, updating inventory, logging an audit trail. The aggregate does not know about the handlers; it only knows how to publish events.

---

## 4. Example in Java

This example models a **stock price ticker** with multiple display views. A `StockTicker` (the subject) tracks the current price for a stock symbol. When the price updates, it notifies all registered observers. Three concrete observers display the data in different ways: a live price panel, a price change indicator, and a high/low tracker.

### Observer Interface

```java
package behavioural.observer;

/**
 * The Observer interface. All observers of a StockTicker
 * must implement this method to receive price updates.
 */
public interface StockObserver {

    /**
     * Called by the subject when the stock price changes.
     *
     * @param ticker the stock symbol (e.g. "AAPL")
     * @param price  the new price
     */
    void onPriceUpdate(String ticker, double price);
}
```

### Subject: `StockTicker`

```java
package behavioural.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Subject (Observable). Maintains a list of observers and
 * notifies them when the stock price changes.
 *
 * Uses CopyOnWriteArrayList to allow safe iteration even if
 * an observer detaches itself during notification.
 */
public class StockTicker {

    private final String symbol;
    private double price;
    private final List<StockObserver> observers = new CopyOnWriteArrayList<>();

    public StockTicker(String symbol, double initialPrice) {
        this.symbol = symbol;
        this.price = initialPrice;
    }

    public void attach(StockObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void detach(StockObserver observer) {
        observers.remove(observer);
    }

    /**
     * Update the stock price and notify all observers.
     * This is the trigger that drives the entire pattern.
     */
    public void setPrice(double newPrice) {
        this.price = newPrice;
        notifyObservers();
    }

    public String getSymbol() { return symbol; }
    public double getPrice()  { return price; }

    private void notifyObservers() {
        for (StockObserver observer : observers) {
            observer.onPriceUpdate(symbol, price);
        }
    }
}
```

### ConcreteObserver: `LivePricePanel`

```java
package behavioural.observer;

/**
 * Displays the current price whenever it updates.
 * A simple, stateless observer.
 */
public class LivePricePanel implements StockObserver {

    private final String panelName;

    public LivePricePanel(String panelName) {
        this.panelName = panelName;
    }

    @Override
    public void onPriceUpdate(String ticker, double price) {
        System.out.printf("[%s] %s: $%.2f%n", panelName, ticker, price);
    }
}
```

### ConcreteObserver: `PriceChangeIndicator`

```java
package behavioural.observer;

/**
 * Tracks the previous price and displays the change direction
 * and magnitude on each update.
 */
public class PriceChangeIndicator implements StockObserver {

    private double previousPrice = -1;

    @Override
    public void onPriceUpdate(String ticker, double price) {
        if (previousPrice < 0) {
            System.out.printf("[ChangeIndicator] %s: initial price $%.2f%n", ticker, price);
        } else {
            double change = price - previousPrice;
            double pct = (change / previousPrice) * 100;
            String arrow = change > 0 ? "UP" : change < 0 ? "DOWN" : "FLAT";
            System.out.printf("[ChangeIndicator] %s: %s %+.2f (%+.2f%%)%n",
                    ticker, arrow, change, pct);
        }
        previousPrice = price;
    }
}
```

### ConcreteObserver: `HighLowTracker`

```java
package behavioural.observer;

/**
 * Tracks the session high and low prices and displays them
 * on each update. Demonstrates an observer that accumulates
 * state over time rather than just reacting to the current value.
 */
public class HighLowTracker implements StockObserver {

    private double high = Double.MIN_VALUE;
    private double low = Double.MAX_VALUE;

    @Override
    public void onPriceUpdate(String ticker, double price) {
        if (price > high) high = price;
        if (price < low) low = price;

        System.out.printf("[HighLow] %s: session high=$%.2f, low=$%.2f%n",
                ticker, high, low);
    }
}
```

### Client Code

```java
package behavioural.observer;

public class App {

    public static void main(String[] args) {
        // Create the subject
        StockTicker apple = new StockTicker("AAPL", 185.50);

        // Create observers
        LivePricePanel mainPanel = new LivePricePanel("MainPanel");
        LivePricePanel mobilePanel = new LivePricePanel("MobilePanel");
        PriceChangeIndicator changeIndicator = new PriceChangeIndicator();
        HighLowTracker highLowTracker = new HighLowTracker();

        // Attach observers to the subject
        apple.attach(mainPanel);
        apple.attach(mobilePanel);
        apple.attach(changeIndicator);
        apple.attach(highLowTracker);

        // Simulate price updates
        System.out.println("=== Price Update 1 ===");
        apple.setPrice(187.25);

        System.out.println();
        System.out.println("=== Price Update 2 ===");
        apple.setPrice(186.00);

        // Detach the mobile panel (user closed the app)
        apple.detach(mobilePanel);

        System.out.println();
        System.out.println("=== Price Update 3 (MobilePanel detached) ===");
        apple.setPrice(189.75);

        System.out.println();
        System.out.println("=== Price Update 4 ===");
        apple.setPrice(184.30);
    }
}
```

### Expected Output

```
=== Price Update 1 ===
[MainPanel] AAPL: $187.25
[MobilePanel] AAPL: $187.25
[ChangeIndicator] AAPL: initial price $187.25
[HighLow] AAPL: session high=$187.25, low=$187.25

=== Price Update 2 ===
[MainPanel] AAPL: $186.00
[MobilePanel] AAPL: $186.00
[ChangeIndicator] AAPL: DOWN -1.25 (-0.67%)
[HighLow] AAPL: session high=$187.25, low=$186.00

=== Price Update 3 (MobilePanel detached) ===
[MainPanel] AAPL: $189.75
[ChangeIndicator] AAPL: UP +3.75 (+2.02%)
[HighLow] AAPL: session high=$189.75, low=$186.00

=== Price Update 4 ===
[MainPanel] AAPL: $184.30
[ChangeIndicator] AAPL: DOWN -5.45 (-2.87%)
[HighLow] AAPL: session high=$189.75, low=$184.30
```

### Why This Works Well

- **Loose coupling.** `StockTicker` knows nothing about panels, change indicators, or trackers. It only knows about the `StockObserver` interface. Adding a new type of display (e.g. a moving-average chart) requires writing one new class and calling `attach()` -- no changes to the subject.
- **Dynamic subscription.** Observers can be added and removed at runtime, as demonstrated by detaching `mobilePanel` mid-session.
- **Push model.** The ticker pushes the symbol and price directly, so observers do not need to call back into the subject. This keeps the notification self-contained.
- **Stateful observers.** `PriceChangeIndicator` and `HighLowTracker` show that observers can accumulate their own state across multiple notifications, not just react to the latest value.
- **Thread-safety consideration.** `CopyOnWriteArrayList` is used for the observer list, which allows safe concurrent iteration and modification. This is a practical choice for real systems where notifications might overlap with subscribe/unsubscribe operations.

---

## 5. Tradeoffs and Limitations

### When to Use It

- **One-to-many state propagation.** When a change in one object must be reflected in multiple others, and you do not want the source to know about the consumers. This is the pattern's primary purpose.
- **Event-driven architectures.** When the system is naturally modelled as events and reactions -- user actions, state transitions, domain events -- and you want to decouple event producers from event handlers.
- **Plugin or extension points.** When you want third-party or module-level code to hook into a system without modifying the core. Registration-based extension is a direct application of Observer.
- **Runtime flexibility.** When the set of interested parties changes during the lifetime of the application (e.g. views opening and closing, services being enabled and disabled).

### When Not to Use It

- **Single consumer.** If only one object ever needs to react to a change, a direct method call is simpler and more explicit. Observer adds indirection that is only justified when the subscriber count is unknown or variable.
- **Synchronous chains are a problem.** In the basic form, `notifyObservers()` calls each observer synchronously and sequentially. If an observer is slow or throws an exception, it blocks (or breaks) notification to subsequent observers. If you need isolation between observers, you need an asynchronous dispatch layer on top of the pattern.
- **Complex event ordering.** The classic pattern provides no guarantees about the order in which observers are notified (beyond list insertion order, which is an implementation detail). If observers have ordering dependencies, you need a more structured event pipeline.

### The Lapsed Listener Problem (Memory Leaks)

This is the most common practical pitfall. If an observer registers with a subject but is never explicitly detached, the subject holds a strong reference to the observer indefinitely. The observer (and everything it references) cannot be garbage-collected, even if no other part of the system uses it.

This is particularly insidious in long-lived subjects (e.g. a global event bus, a singleton service) with short-lived observers (e.g. UI components that come and go). The symptom is a slow memory leak that only manifests under sustained use.

Mitigations:

- **Explicit lifecycle management.** Always detach observers when they are no longer needed. In GUI frameworks, this means unregistering listeners in `dispose()` or `onDestroy()` methods.
- **Weak references.** Store observers using `WeakReference<StockObserver>` (or use a `WeakHashMap` keyed by observer). When the observer is no longer strongly reachable, it becomes eligible for GC and the subject can prune the dead reference on the next notification. The tradeoff is that observers can silently disappear if the caller does not maintain a strong reference.
- **Disposable subscriptions.** Reactive libraries (RxJava, Reactor) return a `Disposable` or `Subscription` handle from `subscribe()`. Calling `dispose()` detaches the observer. `CompositeDisposable` groups multiple subscriptions for bulk cleanup.

### Threading and Concurrency

The basic Observer pattern assumes single-threaded execution. In a concurrent environment, several issues arise:

- **Concurrent modification.** An observer may be added or removed while `notifyObservers()` is iterating. Using `CopyOnWriteArrayList` (as in the example) is a simple solution for read-heavy scenarios. For write-heavy scenarios, consider explicit synchronisation or concurrent data structures.
- **Notification on which thread?** If `setPrice()` is called from a background thread, observers receive the callback on that thread. GUI observers typically need to be dispatched to the UI thread (e.g. `Platform.runLater()` in JavaFX, `SwingUtilities.invokeLater()` in Swing).
- **Reentrant notifications.** An observer's `update()` method might modify the subject, triggering a re-entrant `notifyObservers()` call. This can cause infinite loops or inconsistent state. Guard against this with a `notifying` flag or by deferring state changes until the current notification round completes.

### Notification Granularity

A blunt "something changed" notification forces every observer to re-examine the entire subject state, even if only one field changed. Finer-grained notifications (e.g. specifying which property changed, or using typed event objects) allow observers to filter irrelevant updates. Java's `PropertyChangeListener` takes this approach, passing a `PropertyChangeEvent` that includes the property name, old value, and new value.

### vs Java's Built-in Support

**`java.util.Observable` / `Observer` (deprecated since Java 9).** These were the JDK's original Observer implementation. `Observable` was a class (not an interface), which forced subjects to extend it -- a significant limitation in a single-inheritance language. The `update()` method accepted `(Observable, Object)`, providing no type safety. It was deprecated because the design was considered too restrictive and error-prone. Do not use it in new code.

**`java.beans.PropertyChangeListener` / `PropertyChangeSupport`.** A more practical built-in option. `PropertyChangeSupport` handles observer management and dispatches `PropertyChangeEvent` objects that include the property name, old value, and new value. This is the pull-like hybrid model: enough context is pushed to let observers filter by property name, and the old/new values are included so observers do not need to query the subject. It is a solid choice for JavaBeans-style objects where you want per-property change notification without bringing in an external library.

**`java.util.concurrent.Flow` (Java 9+).** The reactive streams interfaces (`Publisher`, `Subscriber`, `Subscription`, `Processor`) standardise the Observer pattern with backpressure. Use these (or a library like Reactor or RxJava that implements them) when you need asynchronous, non-blocking, flow-controlled event streams.

### Relationship to Other Patterns

| Pattern | Relationship |
|---|---|
| **Mediator** | Mediator centralises communication between objects; Observer decentralises it. A Mediator can use Observer internally to listen for events from its colleagues, but the intent is different: Mediator reduces many-to-many dependencies to a star topology, while Observer creates a clean one-to-many broadcast. |
| **Command** | Commands can be used as the event objects in an Observer system. Instead of a simple callback, the subject enqueues Command objects that observers (or a dispatcher) execute. This adds undo/redo and serialisation capabilities. |
| **Strategy** | Observer and Strategy both decouple behaviour via interfaces, but Strategy replaces an algorithm within a single object, while Observer notifies multiple external objects about state changes. |
| **Chain of Responsibility** | Chain passes a request along a chain until one handler processes it. Observer broadcasts to all registered observers. Chain is "find the right handler"; Observer is "tell everyone". |
