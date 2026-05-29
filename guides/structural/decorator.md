# Decorator Pattern

**Classification:** Structural (GoF)

---

## 1. Core Idea

The Decorator pattern attaches additional responsibilities to an object dynamically, providing a flexible alternative to subclassing for extending functionality.

### The Problem

You have a core object whose behaviour you need to extend in various combinations. Using inheritance, each combination of features requires its own subclass. If you have three optional features -- compression, encryption, and buffering -- you would need seven subclasses to cover every combination (plus the base). Adding a fourth feature doubles that number again. This is the classic **combinatorial explosion** problem with inheritance-based extension.

### The Key Insight

Instead of embedding all possible behaviours in a class hierarchy, wrap the core object in one or more **decorator** objects that each add a single responsibility. Each decorator conforms to the same interface as the object it wraps, so decorators can be stacked in any order. The client code never knows whether it is talking to the core object or a decorated version -- it only sees the shared interface.

This is the **Open/Closed Principle** in action: the core class is closed for modification but open for extension via composition.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Component** | The interface (or abstract class) that defines the operations available on both the core object and its decorators. |
| **ConcreteComponent** | The base object that provides the default implementation of the Component interface. This is the thing being decorated. |
| **Decorator** | An abstract class (or interface) that implements Component and holds a reference to a Component. It delegates all calls to the wrapped component. |
| **ConcreteDecorator** | Extends Decorator, adding its specific behaviour before or after delegating to the wrapped component. |

### How They Compose

```
         <<interface>>
           Component
          /         \
ConcreteComponent   Decorator (has-a Component)
                    /        \
          ConcreteDecoratorA  ConcreteDecoratorB
```

The critical relationship is that `Decorator` both **implements** `Component` and **holds a reference** to a `Component`. This recursive composition is what allows decorators to wrap other decorators transparently.

A decorated call chain looks like this at runtime:

```
Client --> DecoratorB --> DecoratorA --> ConcreteComponent
```

Each decorator performs its own logic and then forwards the call inward to the wrapped component. The response travels back out through the same chain, so decorators can also post-process results.

---

## 3. Use Cases

### Java I/O Streams

The canonical real-world example. `java.io` is built on the Decorator pattern:

```java
InputStream in = new BufferedInputStream(
    new GZIPInputStream(
        new FileInputStream("data.gz")
    )
);
```

`FileInputStream` is the ConcreteComponent. `GZIPInputStream` and `BufferedInputStream` are ConcreteDecorators. They all share the `InputStream` interface. You compose exactly the pipeline you need without any "BufferedGZIPFileInputStream" class.

### Middleware / Filter Chains

Web frameworks like Spring and Express use decorator-like patterns for HTTP middleware. Each middleware wraps the next handler, adding behaviour such as authentication, logging, rate limiting, or CORS headers. The request passes through the chain; each layer can modify the request, modify the response, or short-circuit the chain entirely.

### Logging and Monitoring Wrappers

You have a service interface and want to add logging, metrics collection, or circuit-breaking without modifying the service implementation. A logging decorator wraps the service, logs the call, delegates to the real implementation, logs the result, and returns it. This keeps cross-cutting concerns out of business logic.

### UI Component Decoration

GUI toolkits use decoration to add scrollbars, borders, or shadows to visual components. A `ScrollDecorator` wraps a `TextView`, adding scroll behaviour while delegating rendering to the inner component.

---

## 4. Example in Java

This example models a pricing system where a base product price can be decorated with discounts, taxes, and service charges. Each decorator modifies the price calculation independently, and they can be stacked in any combination.

### The Component Interface

```java
public interface PricingStrategy {

    /**
     * Calculate the final price for a given base amount.
     */
    double calculate(double baseAmount);

    /**
     * Return a human-readable breakdown of how the price was computed.
     */
    String describe();
}
```

### The ConcreteComponent

```java
public class StandardPricing implements PricingStrategy {

    @Override
    public double calculate(double baseAmount) {
        return baseAmount;
    }

    @Override
    public String describe() {
        return "Base price";
    }
}
```

### The Abstract Decorator

```java
public abstract class PricingDecorator implements PricingStrategy {

    protected final PricingStrategy wrapped;

    protected PricingDecorator(PricingStrategy wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public double calculate(double baseAmount) {
        return wrapped.calculate(baseAmount);
    }

    @Override
    public String describe() {
        return wrapped.describe();
    }
}
```

### ConcreteDecorator: Percentage Discount

```java
public class DiscountDecorator extends PricingDecorator {

    private final double discountPercent;
    private final String reason;

    public DiscountDecorator(PricingStrategy wrapped, double discountPercent, String reason) {
        super(wrapped);
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Discount must be between 0 and 100");
        }
        this.discountPercent = discountPercent;
        this.reason = reason;
    }

    @Override
    public double calculate(double baseAmount) {
        double price = super.calculate(baseAmount);
        return price * (1 - discountPercent / 100.0);
    }

    @Override
    public String describe() {
        return super.describe() + String.format(" -> %.0f%% discount (%s)", discountPercent, reason);
    }
}
```

### ConcreteDecorator: Tax

```java
public class TaxDecorator extends PricingDecorator {

    private final double taxPercent;
    private final String taxName;

    public TaxDecorator(PricingStrategy wrapped, double taxPercent, String taxName) {
        super(wrapped);
        this.taxPercent = taxPercent;
        this.taxName = taxName;
    }

    @Override
    public double calculate(double baseAmount) {
        double price = super.calculate(baseAmount);
        return price * (1 + taxPercent / 100.0);
    }

    @Override
    public String describe() {
        return super.describe() + String.format(" -> +%.0f%% %s", taxPercent, taxName);
    }
}
```

### ConcreteDecorator: Fixed Service Charge

```java
public class ServiceChargeDecorator extends PricingDecorator {

    private final double charge;

    public ServiceChargeDecorator(PricingStrategy wrapped, double charge) {
        super(wrapped);
        this.charge = charge;
    }

    @Override
    public double calculate(double baseAmount) {
        double price = super.calculate(baseAmount);
        return price + charge;
    }

    @Override
    public String describe() {
        return super.describe() + String.format(" -> +$%.2f service charge", charge);
    }
}
```

### Client Code

```java
public class PricingDemo {

    public static void main(String[] args) {
        double baseAmount = 100.00;

        // Scenario 1: No decoration -- just the base price
        PricingStrategy plain = new StandardPricing();
        printPrice("Plain", plain, baseAmount);

        // Scenario 2: 10% loyalty discount, then 20% VAT
        PricingStrategy discountedWithTax = new TaxDecorator(
            new DiscountDecorator(
                new StandardPricing(),
                10, "loyalty"
            ),
            20, "VAT"
        );
        printPrice("Discounted + VAT", discountedWithTax, baseAmount);

        // Scenario 3: 20% VAT, then 10% loyalty discount (different result -- order matters)
        PricingStrategy taxThenDiscount = new DiscountDecorator(
            new TaxDecorator(
                new StandardPricing(),
                20, "VAT"
            ),
            10, "loyalty"
        );
        printPrice("VAT + Discounted", taxThenDiscount, baseAmount);

        // Scenario 4: Stack everything -- discount, tax, and service charge
        PricingStrategy fullPipeline = new ServiceChargeDecorator(
            new TaxDecorator(
                new DiscountDecorator(
                    new StandardPricing(),
                    15, "seasonal sale"
                ),
                20, "VAT"
            ),
            5.00
        );
        printPrice("Full pipeline", fullPipeline, baseAmount);
    }

    private static void printPrice(String label, PricingStrategy strategy, double baseAmount) {
        System.out.printf("[%s]%n", label);
        System.out.printf("  Breakdown: %s%n", strategy.describe());
        System.out.printf("  Final price: $%.2f%n%n", strategy.calculate(baseAmount));
    }
}
```

### Expected Output

```
[Plain]
  Breakdown: Base price
  Final price: $100.00

[Discounted + VAT]
  Breakdown: Base price -> 10% discount (loyalty) -> +20% VAT
  Final price: $108.00

[VAT + Discounted]
  Breakdown: Base price -> +20% VAT -> 10% discount (loyalty)
  Final price: $108.00

[Full pipeline]
  Breakdown: Base price -> 15% discount (seasonal sale) -> +20% VAT -> +$5.00 service charge
  Final price: $107.00
```

Notice that in scenarios 2 and 3, the discount and tax happen to produce the same numeric result (because multiplication is commutative), but the `describe()` output shows the different ordering. For non-commutative operations -- such as a fixed-amount discount followed by a percentage tax versus the reverse -- ordering would produce genuinely different final prices. This is a real concern covered in the tradeoffs section below.

---

## 5. Tradeoffs and Limitations

### When to Use the Decorator Pattern

- **You need to add responsibilities to individual objects, not entire classes.** Decoration is per-instance.
- **You need to combine behaviours in many different configurations.** Decorators avoid the combinatorial subclass explosion.
- **You want to follow the Open/Closed Principle.** New behaviours become new decorator classes -- no existing code changes.
- **The extensions are transparent to clients.** Clients should not need to know whether an object is decorated or not.

### When Not to Use It

- **When you have only one or two fixed extensions.** If you will never need to mix and match, simple inheritance or composition is clearer and less abstract.
- **When identity matters.** A decorated object is not the same instance as the inner object. Code that relies on `==` identity checks or `instanceof` against the ConcreteComponent type will break. For example, `decoratedWidget instanceof TextField` will return `false` even though there is a `TextField` inside the wrapper. This is sometimes called the **identity crisis** of decorators.
- **When the interface is large.** The abstract Decorator must delegate every method in the Component interface. If the interface has 30 methods but your decorator only modifies one, you still have to write (or inherit) forwarding for all 30. This is tedious and error-prone. Consider using a dynamic proxy or the Strategy pattern instead.

### Decorator Ordering

Decorator ordering can change behaviour. In the pricing example, applying a fixed-dollar discount before tax yields a different result than applying it after tax. This is by design -- it gives you control -- but it also means the assembly point (where decorators are stacked) must understand the semantics.

Common strategies for managing ordering:
- **Builder or factory methods** that enforce a canonical decoration order.
- **Documentation and convention** -- e.g. "discounts are always innermost, taxes outermost."
- **Priority-based sorting** if decorators are assembled dynamically (as in middleware chains).

### Decorator vs Inheritance

| | Decorator | Inheritance |
|---|---|---|
| **Extension granularity** | Per-instance, at runtime | Per-class, at compile time |
| **Combinatorial flexibility** | Stack freely | Subclass per combination |
| **Transparency to client** | Full (same interface) | Full (same superclass) |
| **Complexity** | Many small objects at runtime | Many classes at compile time |
| **Debugging** | Harder -- deep wrapping chains obscure the call stack | Easier -- single class hierarchy |
| **Access to internals** | Only through the Component interface | Full access to protected members |

### Decorator vs Strategy

If you only need to swap out **one** behaviour, the Strategy pattern (inject a strategy object) is often simpler than wrapping the entire interface. Decorators shine when you need to **layer multiple orthogonal behaviours** on top of each other.

### Practical Tips

- **Keep the Component interface small.** Every method must be forwarded by the abstract decorator. A lean interface makes decoration practical.
- **Use the abstract Decorator class.** Do not make each ConcreteDecorator implement the full interface independently -- centralize the forwarding logic.
- **Be careful with state.** If the ConcreteComponent has mutable state, decorators that cache or transform results can become stale. Ensure decorators delegate to the live object rather than caching eagerly.
- **Name decorators after what they add**, not what they wrap. `BufferedInputStream` tells you it adds buffering. `BufferedFileInputStream` would wrongly couple it to files.
