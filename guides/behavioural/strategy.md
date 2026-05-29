# Strategy Pattern

**Classification:** Behavioural (GoF)

---

## 1. Core Idea

The Strategy pattern defines a family of algorithms, encapsulates each one behind a common interface, and makes them interchangeable. It lets the algorithm vary independently of the clients that use it.

### The Problem

You have an operation that can be performed in several different ways, and the choice of which way depends on runtime conditions -- user preferences, configuration, input characteristics, or business rules. The naive approach is a chain of `if/else` or `switch` statements inside the method that performs the operation:

```java
if (carrier.equals("ROYAL_MAIL")) {
    // 30 lines of Royal Mail rate calculation
} else if (carrier.equals("DPD")) {
    // 25 lines of DPD rate calculation
} else if (carrier.equals("FEDEX")) {
    // 40 lines of FedEx rate calculation
} else {
    throw new IllegalArgumentException("Unknown carrier");
}
```

This is problematic in several ways. The method grows without bound as new algorithms are added. Every algorithm shares scope with every other, making it easy to accidentally share or shadow variables. Testing one algorithm means constructing the context needed by the entire method. Adding a new algorithm means modifying existing code -- a direct violation of the Open/Closed Principle.

### The Key Insight

Extract each algorithm into its own class behind a shared interface. The object that needs the algorithm (the **Context**) holds a reference to the interface, not to any specific implementation. Swapping algorithms becomes a matter of injecting a different implementation -- at construction time, through a setter, or via a factory -- with zero conditional logic in the Context.

The Context delegates the work; the Strategy does the work. The Context does not know or care which concrete strategy it holds.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Strategy** | An interface (or abstract class) that declares the operation all concrete strategies must implement. |
| **ConcreteStrategy** | A class that implements the Strategy interface with a specific algorithm. There are typically several of these. |
| **Context** | The object that has a job to do but delegates the variable part of that job to a Strategy reference. It may provide data to the strategy or let the strategy call back into it. |

### How They Interact

```
        Context
          |
          | has-a
          v
     <<interface>>
       Strategy
       /      \
ConcreteA    ConcreteB    ConcreteC
```

1. The **client** creates or selects a ConcreteStrategy and passes it to the Context (via constructor, setter, or method parameter).
2. When the Context needs the algorithm, it calls the Strategy interface method.
3. The ConcreteStrategy executes its algorithm and returns the result.
4. If the strategy needs data from the Context, the Context can either pass it as parameters or pass a reference to itself (`this`), letting the strategy pull what it needs.

The Context never uses `instanceof`, `switch`, or any conditional logic to choose between algorithms. That decision is made externally and injected.

---

## 3. Use Cases

### Sorting Algorithms

A collection framework might accept a `Comparator<T>` to control sort order. The sort method (the Context) does not know whether elements are being compared by name, by date, by price, or by a compound key. Each comparator is a strategy. Java's `Collections.sort(list, comparator)` is the textbook example of Strategy in the standard library.

### Compression Strategies

A file archiver needs to support multiple compression algorithms -- gzip, bzip2, LZ4, zstd. Each algorithm has radically different performance and compression-ratio characteristics, but the archiver's workflow (read input, compress, write output) is the same. Each compression algorithm is a ConcreteStrategy behind a common `CompressionStrategy` interface.

### Pricing and Discount Rules

An e-commerce platform applies different discount rules depending on the customer tier, promotional campaigns, or seasonal events. Rather than hardcoding the discount logic into the checkout flow, each discount rule is a strategy. The checkout service (Context) delegates price adjustment to whichever strategy is active, and new promotions can be deployed without modifying the checkout code.

### Authentication Methods

A security module supports multiple authentication mechanisms -- username/password, OAuth 2.0, API key, SAML. The authentication flow (receive credentials, validate, return principal) is uniform, but each mechanism validates differently. Each mechanism is a strategy, and the security module switches between them based on the incoming request type or configuration.

### Shipping Cost Calculation

An order fulfilment system supports multiple carriers, each with its own rate card, weight tiers, surcharges, and delivery-time calculations. The order service needs a shipping cost but should not embed carrier-specific logic. Each carrier's rate calculation is a strategy.

---

## 4. Example in Java

This example models a shipping cost calculator. An `Order` is shipped via a carrier, and each carrier has its own pricing algorithm. The system must support adding new carriers without modifying the order processing code.

### The Strategy Interface

```java
package behavioural.strategy.shipping;

/**
 * Strategy interface for calculating shipping cost.
 * Each carrier implements this with its own rate logic.
 */
public interface ShippingCostStrategy {

    /**
     * Calculate the shipping cost for a parcel.
     *
     * @param weightKg    parcel weight in kilograms
     * @param distanceKm  delivery distance in kilometres
     * @return shipping cost in GBP
     */
    double calculate(double weightKg, double distanceKm);

    /**
     * Return the carrier name for display purposes.
     */
    String carrierName();
}
```

### ConcreteStrategy: Royal Mail

```java
package behavioural.strategy.shipping;

/**
 * Royal Mail: flat rate per kg with a fixed handling fee.
 * Economical for lightweight parcels over short distances.
 */
public class RoyalMailStrategy implements ShippingCostStrategy {

    private static final double RATE_PER_KG = 1.50;
    private static final double HANDLING_FEE = 2.99;

    @Override
    public double calculate(double weightKg, double distanceKm) {
        return (weightKg * RATE_PER_KG) + HANDLING_FEE;
    }

    @Override
    public String carrierName() {
        return "Royal Mail";
    }
}
```

### ConcreteStrategy: DPD

```java
package behavioural.strategy.shipping;

/**
 * DPD: rate depends on both weight and distance, with a
 * surcharge for heavy parcels over 20 kg.
 */
public class DpdStrategy implements ShippingCostStrategy {

    private static final double BASE_RATE = 3.50;
    private static final double RATE_PER_KG = 0.80;
    private static final double RATE_PER_KM = 0.02;
    private static final double HEAVY_SURCHARGE = 5.00;
    private static final double HEAVY_THRESHOLD_KG = 20.0;

    @Override
    public double calculate(double weightKg, double distanceKm) {
        double cost = BASE_RATE + (weightKg * RATE_PER_KG) + (distanceKm * RATE_PER_KM);
        if (weightKg > HEAVY_THRESHOLD_KG) {
            cost += HEAVY_SURCHARGE;
        }
        return cost;
    }

    @Override
    public String carrierName() {
        return "DPD";
    }
}
```

### ConcreteStrategy: FedEx Express

```java
package behavioural.strategy.shipping;

/**
 * FedEx Express: premium service with tiered distance pricing
 * and a guaranteed-delivery surcharge.
 */
public class FedExExpressStrategy implements ShippingCostStrategy {

    private static final double BASE_RATE = 8.00;
    private static final double RATE_PER_KG = 1.20;

    @Override
    public double calculate(double weightKg, double distanceKm) {
        double distanceCharge;
        if (distanceKm <= 50) {
            distanceCharge = 2.00;
        } else if (distanceKm <= 200) {
            distanceCharge = 5.00;
        } else {
            distanceCharge = 5.00 + ((distanceKm - 200) * 0.03);
        }
        return BASE_RATE + (weightKg * RATE_PER_KG) + distanceCharge;
    }

    @Override
    public String carrierName() {
        return "FedEx Express";
    }
}
```

### The Context

```java
package behavioural.strategy.shipping;

/**
 * Context: uses a ShippingCostStrategy to calculate shipping
 * for an order. The strategy can be swapped at any time.
 */
public class ShippingCostCalculator {

    private ShippingCostStrategy strategy;

    public ShippingCostCalculator(ShippingCostStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(ShippingCostStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Calculate and return the shipping cost for a parcel.
     */
    public double calculateCost(double weightKg, double distanceKm) {
        return strategy.calculate(weightKg, distanceKm);
    }

    /**
     * Print a shipping quote to stdout.
     */
    public void printQuote(double weightKg, double distanceKm) {
        double cost = strategy.calculate(weightKg, distanceKm);
        System.out.printf("  %-15s  %.1f kg, %,.0f km  ->  £%.2f%n",
                strategy.carrierName(), weightKg, distanceKm, cost);
    }
}
```

### Client Code

```java
package behavioural.strategy.shipping;

import java.util.List;

public class ShippingDemo {

    public static void main(String[] args) {
        double weightKg = 12.5;
        double distanceKm = 150;

        System.out.println("=== Shipping quotes ===");
        System.out.printf("  Parcel: %.1f kg, %,.0f km%n%n", weightKg, distanceKm);

        // Compare all carriers for the same parcel
        ShippingCostCalculator calculator = new ShippingCostCalculator(new RoyalMailStrategy());
        calculator.printQuote(weightKg, distanceKm);

        calculator.setStrategy(new DpdStrategy());
        calculator.printQuote(weightKg, distanceKm);

        calculator.setStrategy(new FedExExpressStrategy());
        calculator.printQuote(weightKg, distanceKm);

        // Programmatic selection: pick the cheapest carrier
        System.out.println("\n=== Cheapest carrier ===");

        List<ShippingCostStrategy> carriers = List.of(
                new RoyalMailStrategy(),
                new DpdStrategy(),
                new FedExExpressStrategy()
        );

        ShippingCostStrategy cheapest = carriers.stream()
                .min((a, b) -> Double.compare(
                        a.calculate(weightKg, distanceKm),
                        b.calculate(weightKg, distanceKm)))
                .orElseThrow();

        System.out.printf("  Winner: %s at £%.2f%n",
                cheapest.carrierName(),
                cheapest.calculate(weightKg, distanceKm));
    }
}
```

### Expected Output

```
=== Shipping quotes ===
  Parcel: 12.5 kg, 150 km

  Royal Mail        12.5 kg, 150 km  ->  £21.74
  DPD               12.5 kg, 150 km  ->  £16.50
  FedEx Express     12.5 kg, 150 km  ->  £28.00

=== Cheapest carrier ===
  Winner: DPD at £16.50
```

### Simplifying with Lambdas (Java 8+)

Because `ShippingCostStrategy` has only one abstract method that matters for the calculation, you can define lightweight strategies as lambdas when the full class is overkill. To make this work cleanly, separate the carrier name from the calculation by using a functional interface for just the cost computation:

```java
@FunctionalInterface
public interface CostFunction {
    double calculate(double weightKg, double distanceKm);
}
```

You can then define a strategy record that pairs a name with a lambda:

```java
public record CarrierStrategy(String carrierName, CostFunction costFunction)
        implements ShippingCostStrategy {

    @Override
    public double calculate(double weightKg, double distanceKm) {
        return costFunction.calculate(weightKg, distanceKm);
    }
}
```

Now strategies can be declared inline without dedicated classes:

```java
ShippingCostStrategy flatRate = new CarrierStrategy(
        "Flat Rate Post",
        (weight, distance) -> 4.99
);

ShippingCostStrategy perKg = new CarrierStrategy(
        "Weight-Based Express",
        (weight, distance) -> 2.00 + weight * 1.75
);
```

This is useful for simple, stateless algorithms. For strategies with complex logic, multiple constants, or their own internal state, dedicated classes remain clearer.

---

## 5. Tradeoffs and Limitations

### When to Use the Strategy Pattern

- **You have multiple algorithms for the same job** and need to switch between them at runtime based on configuration, user choice, or input characteristics.
- **You want to eliminate conditional logic** (`if/else`, `switch`) that selects between algorithms inside a method. Each branch becomes a ConcreteStrategy.
- **You need to test algorithms in isolation.** Each strategy is a standalone class with its own unit tests, decoupled from the Context.
- **You anticipate new algorithms being added over time.** New strategies can be introduced without modifying existing Context or strategy code (Open/Closed Principle).
- **Different clients need different default algorithms.** Each client can be configured with its own strategy instance.

### When Not to Use It

- **You have exactly one algorithm that never varies.** The indirection of an interface and injection adds complexity for no benefit.
- **The algorithm set is tiny, stable, and unlikely to grow.** If there will only ever be two options and the logic is three lines each, a simple `if/else` is more readable than three classes and an interface.
- **The algorithms need deep access to Context internals.** If strategies require access to many private fields of the Context, the interface between them becomes wide and awkward. At that point, consider whether the algorithm truly belongs outside the Context.

### Strategy Selection Responsibility

The Strategy pattern deliberately does not specify *who* chooses the strategy. This is a design decision that must be made per use case:

- **The client chooses directly** -- the most common approach. The client knows the situation and injects the right strategy. Simple, explicit, but couples the client to concrete strategy classes.
- **A factory or registry chooses** -- the client provides a key (e.g. a carrier code string) and a factory returns the appropriate strategy. This decouples the client from concrete classes but introduces a mapping layer.
- **Configuration or dependency injection** -- the strategy is wired in at startup via a DI container. Good for application-wide defaults; less suitable for per-request switching.

Whichever approach you use, keep the selection logic in one place. If strategy selection is scattered across the codebase, you have replaced conditional logic in the algorithm with conditional logic in the wiring, which is not much of an improvement.

### Strategy vs State

Strategy and State are structurally identical -- both have a Context that delegates to an interface with multiple implementations. The difference is in **intent and lifecycle**:

| | Strategy | State |
|---|---|---|
| **Intent** | Swap algorithms | Model state transitions |
| **Who triggers the change** | The client (externally) | The state objects themselves (internally) |
| **Awareness of alternatives** | ConcreteStrategies are unaware of each other | ConcreteStates often know which state to transition to next |
| **Typical lifetime** | Set once or changed rarely | Changes frequently as the object progresses through states |

If the object's behaviour changes because the *client* decided to use a different algorithm, that is Strategy. If the object's behaviour changes because an *internal event* moved it to a new state, that is State.

### Strategy vs Simple Lambdas

In modern Java (8+), any single-method strategy interface is a functional interface, and concrete strategies can be replaced by lambdas. This raises the question: do you still need the pattern?

**Use lambdas when:**
- The algorithm is stateless and fits in one or two lines.
- You do not need to identify or introspect the strategy at runtime (no `carrierName()` or `toString()` needed).
- You do not need to share or reuse the strategy across multiple sites.

**Use full classes when:**
- The algorithm has internal state, constants, or configuration.
- You need additional methods beyond the core operation (naming, description, validation).
- The algorithm is complex enough that a named class aids readability and testability.
- You want to use dependency injection to wire strategies.

Lambdas and the Strategy pattern are not in opposition. Lambdas are a compact way to define ConcreteStrategies. The structural intent of the pattern -- Context delegates to an interchangeable interface -- remains the same regardless of whether the implementation is a class or a lambda.

### Practical Tips

- **Keep the Strategy interface focused.** A strategy with five methods is hard to implement and hard to swap. Ideally it has one method; two or three at most.
- **Favour constructor injection over setters** when the strategy does not change after construction. This makes the Context easier to reason about and thread-safe by default.
- **Consider an enum of strategies** when the set of algorithms is fixed and known at compile time. Each enum constant can implement the strategy method directly, combining type safety with the Strategy pattern's structure.
- **Avoid passing the entire Context to the strategy.** Pass only the data the strategy needs. A wide coupling between Context and Strategy makes strategies hard to reuse in other contexts.
- **Document which strategy is the default** and why. When a system has many interchangeable strategies, it should be clear what happens when no explicit choice is made.
