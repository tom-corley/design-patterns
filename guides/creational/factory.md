# Factory Method Pattern

**Classification:** Creational (GoF)

**Also known as:** Virtual Constructor

---

## 1. Core Idea

The Factory Method pattern defines an interface for creating an object but lets subclasses decide which class to instantiate. Instead of calling a constructor directly, client code calls a method on a creator class, and the creator's subclasses override that method to return different product types.

### The Problem

Imagine you are building a document processing system. You have a `Document` class and an `Application` class that creates and manages documents. The application knows *when* a document should be created (on "File > New", on startup, etc.) but the *kind* of document depends on which specific application is running -- a spreadsheet app creates spreadsheets, a text editor creates text documents.

If you hard-code the constructor call (`new SpreadsheetDocument()`) inside the base `Application`, it becomes impossible to extend the system with new document types without modifying that base class. Every new product type forces a change in the creator, violating the Open/Closed Principle.

### The Key Insight

Pull the object-creation call out into a dedicated method -- the "factory method" -- and make it abstract (or give it a default). Subclasses override this single method to control which concrete class gets instantiated, while the rest of the creator's logic (the workflow that *uses* the product) remains unchanged and inherited.

This gives you a seam: the point where creation happens is decoupled from the logic that consumes the created object. The creator programs to the `Product` interface, and the concrete wiring happens in subclasses.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Product** | The interface (or abstract class) that defines the contract for objects the factory method creates. |
| **ConcreteProduct** | A specific implementation of `Product`. |
| **Creator** | Declares the factory method, which returns a `Product`. May also define a default implementation. Contains business logic that *uses* the product via the `Product` interface. |
| **ConcreteCreator** | Overrides the factory method to return a specific `ConcreteProduct`. |

### Relationships

```
          +-------------------+              +-------------------+
          |     Creator       |              |     Product       |
          |-------------------|              |-------------------|
          | ...               |              | + operation()     |
          | + factoryMethod() |--- creates -->|                   |
          | + someOperation() |              +-------------------+
          +-------------------+                      ^
                  ^                                  |
                  |                                  |
    +-------------------------+         +-------------------------+
    |    ConcreteCreator      |         |    ConcreteProduct      |
    |-------------------------|         |-------------------------|
    | + factoryMethod()       |-------->| + operation()           |
    |   returns new           |         +-------------------------+
    |   ConcreteProduct()     |
    +-------------------------+
```

The `Creator` class has a method `someOperation()` that calls `factoryMethod()` to get a `Product`, then works with it through the `Product` interface. The `ConcreteCreator` overrides `factoryMethod()` to return the appropriate `ConcreteProduct`. The creator never needs to know the concrete class.

---

## 3. Use Cases

**Framework and library extension points.** A framework defines an abstract `Application` class with a `createDocument()` factory method. Users of the framework subclass `Application` and override `createDocument()` to plug in their own document types without modifying framework code. This is the canonical GoF motivation.

**Cross-platform UI toolkits.** A dialog class needs to create buttons, but the concrete button type depends on the operating system (Windows, macOS, Linux). Each platform-specific dialog subclass overrides the factory method to return the platform-appropriate button. The dialog's layout and event-handling logic stays in the base class.

**Notification dispatch systems.** A notification service defines how to format, throttle, and deliver notifications. The *channel* (email, SMS, push notification, Slack webhook) varies. Each channel-specific subclass overrides the factory method that creates the channel-appropriate transport, while the orchestration logic is inherited.

**Data persistence layers.** A repository base class encapsulates query logic and caching. Subclasses override a factory method to return the appropriate database connection or query builder (Postgres, MySQL, in-memory for tests). The repository's read/write operations work against the returned abstraction.

**Test doubles and dependency injection.** Factory methods are natural seams for testing. A test subclass can override the factory method to return a mock or stub, giving you dependency injection without a DI container.

---

## 4. Example in Java

This example models a payment processing system. A `PaymentProcessor` (the Creator) handles the workflow of validating, executing, and logging a payment. The specific payment gateway (Stripe, PayPal, etc.) is determined by which subclass you instantiate.

### Product interface

```java
/**
 * Product interface. All payment gateways must implement this contract.
 */
public interface PaymentGateway {

    /**
     * Charge the given amount (in minor units, e.g. cents) against the
     * provided payment token.
     *
     * @param paymentToken an opaque token representing the customer's payment method
     * @param amountInCents the amount to charge
     * @return a transaction reference ID on success
     */
    String charge(String paymentToken, long amountInCents);

    /**
     * Refund a previously completed transaction.
     *
     * @param transactionId the ID returned by {@link #charge}
     * @return true if the refund was accepted
     */
    boolean refund(String transactionId);

    /** Human-readable name of the gateway, used in logs and receipts. */
    String gatewayName();
}
```

### Concrete products

```java
public class StripeGateway implements PaymentGateway {

    @Override
    public String charge(String paymentToken, long amountInCents) {
        // In production this would call the Stripe API.
        String transactionId = "stripe_txn_" + System.nanoTime();
        System.out.println("[Stripe] Charged " + amountInCents
                + " cents on token " + paymentToken
                + " -> " + transactionId);
        return transactionId;
    }

    @Override
    public boolean refund(String transactionId) {
        System.out.println("[Stripe] Refunded transaction " + transactionId);
        return true;
    }

    @Override
    public String gatewayName() {
        return "Stripe";
    }
}
```

```java
public class PayPalGateway implements PaymentGateway {

    @Override
    public String charge(String paymentToken, long amountInCents) {
        String transactionId = "paypal_txn_" + System.nanoTime();
        System.out.println("[PayPal] Charged " + amountInCents
                + " cents on token " + paymentToken
                + " -> " + transactionId);
        return transactionId;
    }

    @Override
    public boolean refund(String transactionId) {
        System.out.println("[PayPal] Refunded transaction " + transactionId);
        return true;
    }

    @Override
    public String gatewayName() {
        return "PayPal";
    }
}
```

### Creator (abstract)

```java
/**
 * Creator. Contains the payment workflow logic and declares the factory method.
 *
 * Note how {@code processPayment} and {@code processRefund} work entirely
 * through the {@link PaymentGateway} interface. They never reference a
 * concrete gateway class.
 */
public abstract class PaymentProcessor {

    /**
     * The factory method. Subclasses override this to supply the
     * concrete gateway.
     */
    protected abstract PaymentGateway createGateway();

    /**
     * High-level payment workflow: validate -> charge -> log.
     * This is the template that benefits from the factory method --
     * the algorithm is fixed, but the gateway varies.
     */
    public String processPayment(String paymentToken, long amountInCents) {
        if (amountInCents <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        PaymentGateway gateway = createGateway();

        System.out.println("Processing payment of " + amountInCents
                + " cents via " + gateway.gatewayName());

        String transactionId = gateway.charge(paymentToken, amountInCents);

        System.out.println("Payment complete. Transaction: " + transactionId);
        return transactionId;
    }

    /**
     * High-level refund workflow.
     */
    public boolean processRefund(String transactionId) {
        PaymentGateway gateway = createGateway();

        System.out.println("Processing refund for " + transactionId
                + " via " + gateway.gatewayName());

        boolean success = gateway.refund(transactionId);

        System.out.println(success ? "Refund succeeded." : "Refund failed.");
        return success;
    }
}
```

### Concrete creators

```java
public class StripePaymentProcessor extends PaymentProcessor {

    @Override
    protected PaymentGateway createGateway() {
        return new StripeGateway();
    }
}
```

```java
public class PayPalPaymentProcessor extends PaymentProcessor {

    @Override
    protected PaymentGateway createGateway() {
        return new PayPalGateway();
    }
}
```

### Client code

```java
public class Main {

    public static void main(String[] args) {
        // The client chooses the processor once; after that, all
        // interaction is through the PaymentProcessor interface.
        PaymentProcessor processor = resolveProcessor("stripe");

        String txnId = processor.processPayment("tok_visa_4242", 2999);
        processor.processRefund(txnId);

        System.out.println("---");

        processor = resolveProcessor("paypal");
        txnId = processor.processPayment("tok_paypal_buyer", 5500);
        processor.processRefund(txnId);
    }

    /**
     * Simulates a configuration-driven selection. In a real system this
     * might read from a config file, environment variable, or DI container.
     */
    private static PaymentProcessor resolveProcessor(String provider) {
        return switch (provider.toLowerCase()) {
            case "stripe"  -> new StripePaymentProcessor();
            case "paypal"  -> new PayPalPaymentProcessor();
            default -> throw new IllegalArgumentException(
                    "Unknown payment provider: " + provider);
        };
    }
}
```

### Expected output

```
Processing payment of 2999 cents via Stripe
[Stripe] Charged 2999 cents on token tok_visa_4242 -> stripe_txn_...
Payment complete. Transaction: stripe_txn_...
Processing refund for stripe_txn_... via Stripe
[Stripe] Refunded transaction stripe_txn_...
Refund succeeded.
---
Processing payment of 5500 cents via PayPal
[PayPal] Charged 5500 cents on token tok_paypal_buyer -> paypal_txn_...
Payment complete. Transaction: paypal_txn_...
Processing refund for paypal_txn_... via PayPal
[PayPal] Refunded transaction paypal_txn_...
Refund succeeded.
```

### Why this works

- Adding a new gateway (e.g. `SquareGateway` + `SquarePaymentProcessor`) requires zero changes to `PaymentProcessor`, `Main`, or any existing gateway class. You add new classes; you modify nothing.
- `PaymentProcessor.processPayment()` is tested once. Gateway-specific behavior is tested in isolation through the `PaymentGateway` interface.
- In tests, you can create a `TestPaymentProcessor` that returns a mock gateway, with no DI framework required.

---

## 5. Tradeoffs and Limitations

### When to use it

- You have a stable workflow (the Creator's template logic) but the specific object it operates on varies and is expected to grow over time.
- You want to follow the Open/Closed Principle: new product types should not require changes to existing creator code.
- You need a clean seam for substituting test doubles without a DI container.
- Framework authors who cannot predict what concrete classes their users will need.

### When not to use it

- **Only one product type exists and is unlikely to change.** The extra class hierarchy adds indirection for no benefit. A direct constructor call is simpler and clearer.
- **The creation logic is complex and shared across creators.** If every concrete creator does the same multi-step initialization, the logic gets duplicated. Consider Abstract Factory or a Builder instead.
- **You need to select the product type at runtime based on data, not subclass identity.** A parameterized factory (simple factory / static factory method) or a registry/map of lambdas is often more practical than a class hierarchy. The GoF Factory Method pattern specifically uses inheritance; if you find yourself creating subclasses solely to pick a product, the pattern is being forced.

### Complexity cost

Every new product type requires a new `ConcreteProduct` *and* a new `ConcreteCreator`. This parallel class hierarchy can bloat a codebase if the number of variants is large. In languages with first-class functions (or Java with lambdas/`Supplier<T>`), you can often achieve the same decoupling with a functional approach and avoid the subclass explosion.

### Alternatives to consider

| Alternative | When to prefer it |
|---|---|
| **Simple Factory / Static Factory Method** | Selection is based on a runtime parameter; you do not need the Creator to carry additional workflow logic. |
| **Abstract Factory** | You need to create *families* of related objects that must be used together (e.g. a UI theme that includes buttons, text fields, and scrollbars). |
| **Builder** | Construction is multi-step or has many optional parameters. |
| **Prototype** | Creating a new object is expensive and cloning an existing instance is cheaper. |
| **Dependency Injection (container)** | Your application already uses a DI framework; wiring is better handled declaratively than through subclassing. |

### Relationship to other patterns

- Factory Method is often the starting point; as complexity grows, designs frequently evolve toward **Abstract Factory** (multiple related products) or **Prototype** (avoiding subclasses entirely by cloning).
- **Template Method** and Factory Method are natural companions. The Creator's workflow method is often a template method that calls the factory method as one of its steps.
- Factory Methods are frequently called from within **Template Methods**, **Strategies**, and **Composites** -- anywhere a class needs to create an object it works with but should not be coupled to a concrete type.
