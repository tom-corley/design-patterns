# Adapter Pattern

**Classification:** Structural (GoF)

---

## 1. Core Idea

The Adapter pattern converts the interface of an existing class into a different interface that a client expects. It allows classes to work together that otherwise could not because of incompatible interfaces.

### The Problem

You have a client that depends on a specific interface, and you have an existing class (or third-party library) whose functionality you need -- but its interface does not match what the client expects. You cannot (or should not) modify either the client or the existing class.

### The Key Insight

Instead of modifying either side, you introduce a thin intermediary -- the adapter -- that translates calls from the target interface into calls on the adaptee. The client programs against the target interface and remains completely unaware that the adaptee exists. This is the same principle as a physical power adapter: the wall socket (adaptee) provides power in one format, your device (client) expects another format, and the adapter sits between them to make the connection work.

The pattern is fundamentally about **interface translation**, not about adding behaviour, simplifying a subsystem, or controlling access. If you find yourself doing those things, you are likely looking at Decorator, Facade, or Proxy instead.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Target** | The interface the client expects to work with. |
| **Adapter** | Implements the Target interface and holds a reference to (or extends) the Adaptee. Translates Target method calls into Adaptee method calls. |
| **Adaptee** | The existing class with a useful but incompatible interface. |
| **Client** | Collaborates with objects through the Target interface. |

### Class Adapter vs Object Adapter

There are two structural variants:

**Object Adapter (composition):** The adapter holds a reference to an instance of the adaptee and delegates calls to it. This is the preferred approach in most situations because it follows composition over inheritance, works when the adaptee is final or has many subclasses, and allows adapting multiple adaptees behind a single target interface.

```
Client --> Target (interface)
               ^
               |
           Adapter ---has-a---> Adaptee
```

**Class Adapter (inheritance):** The adapter extends the adaptee and implements the target interface simultaneously. This requires multiple inheritance (or in Java, extending a class and implementing an interface). It gives the adapter direct access to the adaptee's protected members and avoids the extra indirection of delegation, but it tightly couples the adapter to one specific adaptee class.

```
Client --> Target (interface)
               ^
               |
           Adapter --extends--> Adaptee
```

In Java, the class adapter approach is only possible when the adaptee is a concrete class (not final) and the target is an interface. Since Java lacks multiple class inheritance, you cannot use a class adapter when both the target and adaptee are concrete classes.

**Prefer the object adapter** unless you have a specific reason to use the class variant. It is more flexible and easier to test.

---

## 3. Use Cases

### Legacy system integration

You are building a new order management system that depends on a clean `InventoryService` interface, but the company's existing inventory system exposes a decades-old SOAP/XML API with completely different method signatures and data formats. An adapter wraps the legacy client and translates between the two interfaces, letting the new system evolve independently.

### Third-party library wrapping

Your application defines its own `Logger` interface. You want to use SLF4J, Log4j, or java.util.logging as the backing implementation without coupling your domain code to any of them. An adapter for each logging library implements your `Logger` interface and delegates to the library-specific calls. Swapping logging frameworks means swapping one adapter for another.

### Payment gateway integration

Your e-commerce platform defines a `PaymentProcessor` interface, but each payment provider (Stripe, PayPal, Adyen) has its own SDK with a distinct API shape. An adapter per provider translates your uniform `PaymentProcessor.charge(...)` call into the provider-specific sequence of API calls.

### Data format conversion

A reporting module expects data as domain objects, but the data source returns raw JSON, CSV rows, or JDBC ResultSets. An adapter reads from the raw format and returns the expected domain objects.

### Testing

You can write an adapter that wraps an external service and implements a target interface. In tests, you swap it out for a stub or mock that implements the same target interface, without touching the client code at all.

---

## 4. Example in Java

### Scenario

An e-commerce platform defines a `PaymentProcessor` interface for charging customers. A legacy in-house billing system (`LegacyBillingSystem`) already handles payments but exposes a completely different API -- different method names, different parameter types, different result format. We write an adapter so the platform can use the legacy system through the modern interface without modifying either side.

### Target interface

```java
/**
 * The interface the e-commerce platform depends on.
 */
public interface PaymentProcessor {

    /**
     * Charge the given amount against a payment method.
     *
     * @param customerId    unique customer identifier
     * @param amountInCents charge amount in minor currency units
     * @param currency      ISO 4217 currency code (e.g. "GBP", "USD")
     * @param paymentToken  tokenised card or payment method reference
     * @return a result indicating success/failure and a transaction reference
     */
    PaymentResult charge(String customerId, long amountInCents, String currency, String paymentToken);
}
```

### Result type

```java
public class PaymentResult {

    private final boolean success;
    private final String transactionId;
    private final String errorMessage;

    private PaymentResult(boolean success, String transactionId, String errorMessage) {
        this.success = success;
        this.transactionId = transactionId;
        this.errorMessage = errorMessage;
    }

    public static PaymentResult success(String transactionId) {
        return new PaymentResult(true, transactionId, null);
    }

    public static PaymentResult failure(String errorMessage) {
        return new PaymentResult(false, null, errorMessage);
    }

    public boolean isSuccess() { return success; }
    public String getTransactionId() { return transactionId; }
    public String getErrorMessage() { return errorMessage; }

    @Override
    public String toString() {
        if (success) {
            return "PaymentResult[OK, txn=" + transactionId + "]";
        }
        return "PaymentResult[FAILED, error=" + errorMessage + "]";
    }
}
```

### Adaptee -- the legacy billing system

```java
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * A legacy in-house billing system with its own conventions.
 * We cannot (or choose not to) modify this class.
 */
public class LegacyBillingSystem {

    /**
     * Processes a payment the old-fashioned way.
     *
     * @param accountCode   internal account code (not the same as customerId)
     * @param amount        charge amount as a double in major currency units
     * @param currencyCode  3-letter currency string
     * @param cardRef       internal card reference
     * @return a map with keys "status" ("OK" or "ERROR") and "ref" (transaction reference)
     */
    public Map<String, String> processPayment(String accountCode, double amount,
                                               String currencyCode, String cardRef) {
        // Simulate payment processing
        Map<String, String> result = new HashMap<>();

        if (amount <= 0) {
            result.put("status", "ERROR");
            result.put("ref", "");
            result.put("message", "Invalid amount: " + amount);
            return result;
        }

        // In reality this would call an internal banking API
        String reference = "LEG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("[LegacyBillingSystem] Charged " + currencyCode + " "
                + String.format("%.2f", amount) + " to account " + accountCode
                + " (card " + cardRef + ") -> ref " + reference);

        result.put("status", "OK");
        result.put("ref", reference);
        result.put("message", "");
        return result;
    }
}
```

### The Adapter (object adapter)

```java
import java.util.Map;

/**
 * Adapts the LegacyBillingSystem to the PaymentProcessor interface.
 *
 * Translation responsibilities:
 *   - customerId  -> accountCode  (prefix with "ACCT-")
 *   - amountInCents (long) -> amount (double, divided by 100)
 *   - paymentToken -> cardRef (prefix with "CARD-")
 *   - Map result  -> PaymentResult
 */
public class LegacyBillingAdapter implements PaymentProcessor {

    private final LegacyBillingSystem legacySystem;

    public LegacyBillingAdapter(LegacyBillingSystem legacySystem) {
        this.legacySystem = legacySystem;
    }

    @Override
    public PaymentResult charge(String customerId, long amountInCents,
                                String currency, String paymentToken) {

        // --- Translate inputs ---
        String accountCode = "ACCT-" + customerId;
        double amount = amountInCents / 100.0;
        String cardRef = "CARD-" + paymentToken;

        // --- Delegate to the adaptee ---
        Map<String, String> legacyResult = legacySystem.processPayment(
                accountCode, amount, currency, cardRef);

        // --- Translate the result ---
        if ("OK".equals(legacyResult.get("status"))) {
            return PaymentResult.success(legacyResult.get("ref"));
        }
        return PaymentResult.failure(legacyResult.get("message"));
    }
}
```

### Client code

```java
/**
 * The e-commerce checkout service. It depends only on PaymentProcessor
 * and knows nothing about LegacyBillingSystem.
 */
public class CheckoutService {

    private final PaymentProcessor paymentProcessor;

    public CheckoutService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    public void checkout(String customerId, long totalCents, String currency, String token) {
        System.out.println("Processing checkout for customer " + customerId
                + " -- " + currency + " " + String.format("%.2f", totalCents / 100.0));

        PaymentResult result = paymentProcessor.charge(customerId, totalCents, currency, token);

        if (result.isSuccess()) {
            System.out.println("Payment successful: " + result.getTransactionId());
        } else {
            System.out.println("Payment failed: " + result.getErrorMessage());
        }
    }
}
```

### Putting it together

```java
public class Main {

    public static void main(String[] args) {
        // The adaptee -- already exists, we don't touch it
        LegacyBillingSystem legacySystem = new LegacyBillingSystem();

        // The adapter -- bridges the gap
        PaymentProcessor processor = new LegacyBillingAdapter(legacySystem);

        // The client -- depends only on PaymentProcessor
        CheckoutService checkout = new CheckoutService(processor);

        // Run a checkout
        checkout.checkout("cust-42", 4999, "GBP", "tok_visa_123");
        System.out.println();
        checkout.checkout("cust-42", -100, "GBP", "tok_visa_123");
    }
}
```

### Expected output

```
Processing checkout for customer cust-42 -- GBP 49.99
[LegacyBillingSystem] Charged GBP 49.99 to account ACCT-cust-42 (card CARD-tok_visa_123) -> ref LEG-A3F7B2C1
Payment successful: LEG-A3F7B2C1

Processing checkout for customer cust-42 -- GBP -1.00
Payment failed: Invalid amount: -1.0
```

*(The transaction reference will differ on each run because it contains a random UUID fragment.)*

---

## 5. Tradeoffs and Limitations

### When to use the Adapter

- You need to use an existing class but its interface does not match what your code expects.
- You want to create a reusable class that cooperates with unrelated or unforeseen classes.
- You are integrating with a third-party library or legacy system and want to isolate your code from its API surface.
- You want to be able to swap implementations (e.g. different payment providers) behind a uniform interface.

### When not to use it

- **The interfaces are already compatible.** If the existing class already satisfies your target interface (or can trivially implement it), an adapter is unnecessary indirection.
- **You need to add behaviour, not translate interfaces.** If you are adding logging, validation, caching, or access control on top of an existing interface, that is Decorator, not Adapter.
- **You are simplifying a complex subsystem.** If multiple classes need to be coordinated behind a simpler interface, that is Facade. An adapter translates one interface to another; a facade simplifies many interfaces into one.
- **You control both sides.** If you own the client and the service and can change either one, consider just making them compatible directly. An adapter is most valuable when at least one side is outside your control.

### Complexity cost

The adapter itself is usually simple -- a thin translation layer. The real cost is:

- **One more class per integration.** In a system with many adaptees (e.g. 10 payment providers), you end up with 10 adapter classes. This is manageable but adds to the surface area.
- **Mapping logic can become non-trivial.** If the adaptee's model is significantly different from the target's model (different error handling, async vs sync, different transaction lifecycles), the adapter can grow thick. At that point, consider whether an anti-corruption layer or a more substantial integration module is more appropriate.
- **Risk of leaky abstractions.** If the adaptee has limitations (timeouts, retry semantics, partial failures) that the target interface does not model, the adapter may paper over important differences. Be deliberate about what the adapter hides and what it exposes.

### Adapter vs Facade

| | Adapter | Facade |
|---|---|---|
| **Intent** | Make an incompatible interface compatible | Simplify a complex subsystem |
| **Number of wrapped classes** | Usually one | Often many |
| **Interface change** | Translates from one shape to another | Provides a new, simpler shape |
| **Existing interface** | Target interface already exists; adaptee must conform to it | Facade defines a new interface |
| **Direction** | Adapts existing code to an existing contract | Creates a new contract over existing code |

They are not mutually exclusive. A facade over a complex subsystem might internally use adapters for individual components within that subsystem.

### Adapter vs Decorator

Both wrap another object, but their intent differs. An adapter changes the interface; a decorator preserves the interface and adds behaviour. If the wrapper implements the same interface as the wrapped object, it is a decorator. If it implements a different interface, it is an adapter.
