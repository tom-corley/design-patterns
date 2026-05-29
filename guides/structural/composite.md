# Composite Pattern

**Classification:** Structural (GoF)

---

## 1. Core Idea

The Composite pattern lets you build tree structures of objects and then work with those trees as if they were individual objects.

### The Problem

Client code that must distinguish between "a single thing" and "a group of things" becomes riddled with conditionals. The Composite pattern eliminates this distinction by defining a common interface that both individual objects (leaves) and containers (composites) implement.

### The Key Insight

A composite object (a container that holds children) exposes the *same interface* as a leaf object. When you call an operation on a composite, it delegates that operation to each of its children -- which may themselves be composites. The client never needs to know whether it is talking to a single object or an entire subtree.

This creates a recursive, part-whole hierarchy where:

- A single item can answer a question (e.g., "what is your price?").
- A group of items can answer the *same* question by aggregating the answers from its children.
- Groups can contain other groups to arbitrary depth.

The pattern is a natural fit whenever your domain model forms a tree and you want uniform treatment of nodes at every level.

---

## 2. Structure

### Participants

| Participant   | Role |
|---------------|------|
| **Component** | The common interface (or abstract class) shared by both leaves and composites. Declares the operation(s) that all elements must support. May declare child-management methods (`add`, `remove`, `getChildren`) here or only on the Composite -- this is a design choice with tradeoffs (see Section 5). |
| **Leaf**      | A terminal node with no children. Implements the Component interface directly -- the operation does real work here. |
| **Composite** | A node that holds a collection of child Components. Implements the Component interface by delegating to its children (typically iterating and aggregating). Also provides methods to add/remove children. |

### How They Relate

```
         Component (interface)
          /            \
         /              \
       Leaf           Composite
                     (has List<Component>)
```

- `Composite` holds a `List<Component>`, so its children can be either `Leaf` or `Composite` instances.
- This recursive composition is what produces the tree structure.
- Client code programs against the `Component` interface and is unaware of whether the underlying object is a leaf or an entire subtree.

### Object Graph (Runtime)

A typical runtime tree might look like this:

```
            Composite (root)
           /        \
      Composite      Leaf
      /      \
   Leaf      Leaf
```

Every node in this tree satisfies the `Component` contract. Calling an operation on the root triggers a recursive traversal of the entire tree.

---

## 3. Use Cases

### File Systems

The classic example. A `File` is a leaf; a `Directory` is a composite that contains files and other directories. Operations like "calculate total size" or "search by name" are defined on the common `FileSystemEntry` interface and work identically whether you call them on a single file or an entire directory tree.

### UI Component Trees

GUI frameworks model views as trees. A `Button` or `Label` is a leaf. A `Panel` or `Container` is a composite that holds other UI components. Operations like `render()`, `resize()`, or `handleEvent()` propagate down the tree. React's component model, Swing's `JComponent` hierarchy, and Android's `ViewGroup`/`View` structure all follow this pattern.

### Organisation Charts

An individual employee is a leaf. A department is a composite containing employees and sub-departments. Operations like "calculate total salary budget" or "count headcount" aggregate recursively through the hierarchy.

### Pricing and Order Systems

A single line item has a price. A bundle (or package deal) is a composite that contains line items and potentially other bundles. Calculating the total price of an order is a single method call regardless of nesting depth -- this is the domain we will use in the example below.

### Document Structure

A `Paragraph` is a leaf. A `Section` is a composite containing paragraphs and sub-sections. A `Document` is a composite containing sections. Operations like "word count" or "export to PDF" propagate uniformly.

---

## 4. Example in Java

This example models a pricing engine for an e-commerce order system. Individual products have a price; bundles group products (and other bundles) together, optionally applying a discount. The `OrderComponent` interface lets client code calculate the total price of any part of the order tree without knowing its structure.

### Component Interface

```java
package structural.composite;

/**
 * The Component interface. Both individual items and bundles implement this.
 */
public interface OrderComponent {

    String getName();

    double getPrice();

    /**
     * Print a structured representation of this component.
     * The indent parameter controls nesting depth for display.
     */
    void print(String indent);
}
```

### Leaf -- ProductItem

```java
package structural.composite;

/**
 * A leaf node: a single product with a fixed price.
 */
public class ProductItem implements OrderComponent {

    private final String name;
    private final double price;

    public ProductItem(String name, double price) {
        this.name = name;
        this.price = price;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getPrice() {
        return price;
    }

    @Override
    public void print(String indent) {
        System.out.printf("%s- %s: $%.2f%n", indent, name, price);
    }
}
```

### Composite -- Bundle

```java
package structural.composite;

import java.util.ArrayList;
import java.util.List;

/**
 * A composite node: a bundle that contains OrderComponents (products or other bundles).
 * Can apply an optional percentage discount to the aggregate price of its children.
 */
public class Bundle implements OrderComponent {

    private final String name;
    private final double discountPercent;
    private final List<OrderComponent> children = new ArrayList<>();

    public Bundle(String name) {
        this(name, 0.0);
    }

    public Bundle(String name, double discountPercent) {
        this.name = name;
        this.discountPercent = discountPercent;
    }

    public void add(OrderComponent component) {
        children.add(component);
    }

    public void remove(OrderComponent component) {
        children.remove(component);
    }

    public List<OrderComponent> getChildren() {
        return children;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * The price of a bundle is the sum of its children's prices,
     * minus any discount applied at this level.
     */
    @Override
    public double getPrice() {
        double total = 0;
        for (OrderComponent child : children) {
            total += child.getPrice();
        }
        double discount = total * (discountPercent / 100.0);
        return total - discount;
    }

    @Override
    public void print(String indent) {
        String discountLabel = discountPercent > 0
                ? String.format(" [%.0f%% bundle discount]", discountPercent)
                : "";
        System.out.printf("%s+ %s%s: $%.2f%n", indent, name, discountLabel, getPrice());
        for (OrderComponent child : children) {
            child.print(indent + "  ");
        }
    }
}
```

### Client Code -- Main

```java
package structural.composite;

/**
 * Demonstrates the Composite pattern with a nested order structure.
 * The client interacts with everything through the OrderComponent interface.
 */
public class Main {

    public static void main(String[] args) {
        // Individual products (leaves)
        ProductItem keyboard = new ProductItem("Mechanical Keyboard", 129.99);
        ProductItem mouse = new ProductItem("Ergonomic Mouse", 79.99);
        ProductItem monitor = new ProductItem("27-inch Monitor", 449.99);
        ProductItem webcam = new ProductItem("HD Webcam", 69.99);
        ProductItem headset = new ProductItem("Wireless Headset", 149.99);
        ProductItem usbHub = new ProductItem("USB-C Hub", 49.99);

        // A peripherals bundle (composite) with a 10% discount
        Bundle peripherals = new Bundle("Peripherals Bundle", 10);
        peripherals.add(keyboard);
        peripherals.add(mouse);
        peripherals.add(usbHub);

        // A video conferencing bundle (composite) with a 5% discount
        Bundle videoKit = new Bundle("Video Conferencing Kit", 5);
        videoKit.add(webcam);
        videoKit.add(headset);

        // The full order (top-level composite) -- no additional discount at this level
        Bundle fullOrder = new Bundle("Complete Workstation Order");
        fullOrder.add(monitor);
        fullOrder.add(peripherals);   // bundle inside a bundle
        fullOrder.add(videoKit);      // another bundle inside a bundle

        // Client code uses the same interface for everything
        System.out.println("=== Full Order ===");
        fullOrder.print("");
        System.out.println();

        // Works identically on a subtree
        System.out.println("=== Peripherals Only ===");
        peripherals.print("");
        System.out.println();

        // Works identically on a single leaf
        System.out.println("=== Single Item ===");
        monitor.print("");
        System.out.println();

        // Demonstrate uniform treatment: calculate price through the same interface
        printPrice(fullOrder);
        printPrice(peripherals);
        printPrice(monitor);
    }

    /**
     * This method accepts any OrderComponent -- it does not need to know
     * whether it received a leaf or an entire tree. This is the core
     * benefit of the Composite pattern.
     */
    private static void printPrice(OrderComponent component) {
        System.out.printf("Total for \"%s\": $%.2f%n", component.getName(), component.getPrice());
    }
}
```

### Expected Output

```
=== Full Order ===
+ Complete Workstation Order: $893.96
  - 27-inch Monitor: $449.99
  + Peripherals Bundle [10% bundle discount]: $233.97
    - Mechanical Keyboard: $129.99
    - Ergonomic Mouse: $79.99
    - USB-C Hub: $49.99
  + Video Conferencing Kit [5% bundle discount]: $209.00
    - HD Webcam: $69.99
    - Wireless Headset: $149.99

=== Peripherals Only ===
+ Peripherals Bundle [10% bundle discount]: $233.97
  - Mechanical Keyboard: $129.99
  - Ergonomic Mouse: $79.99
  - USB-C Hub: $49.99

=== Single Item ===
- 27-inch Monitor: $449.99

Total for "Complete Workstation Order": $893.96
Total for "Peripherals Bundle": $233.97
Total for "27-inch Monitor": $449.99
```

Notice that `printPrice()` takes an `OrderComponent` and works uniformly on a leaf, a shallow bundle, or a deeply nested order. The client is completely decoupled from the tree structure.

---

## 5. Tradeoffs and Limitations

### When to Use It

- Your domain naturally forms a **tree or part-whole hierarchy**.
- You want clients to **treat individual objects and compositions uniformly** -- one interface, no `instanceof` checks.
- Operations on the tree are **aggregations** (sum, count, search, render) that decompose naturally into recursive calls.
- The tree structure can vary at runtime (children are added/removed dynamically).

### When to Avoid It

- **Flat collections are simpler.** If you only have one level of grouping (e.g., a list of items), a plain `List` is clearer than a Composite. Do not introduce the pattern for structures that are not genuinely recursive.
- **Heterogeneous operations.** If leaves and composites need fundamentally different interfaces (not just different implementations of the same operation), forcing them into a shared interface creates awkward no-op methods or exceptions.
- **Performance-critical tight loops.** The recursive delegation introduces overhead. For very large trees where you need raw performance, a flat array with index-based parent-child relationships may be more appropriate.

### Type Safety Concerns

A key design tension in the Composite pattern is **where to declare child-management methods** (`add`, `remove`, `getChildren`):

| Approach | Pros | Cons |
|----------|------|------|
| **On the Component interface** (transparency) | Maximum uniformity -- clients never need to downcast. | Leaves must implement `add`/`remove` with no-ops or exceptions. Calling `add` on a leaf is a runtime error, not a compile-time error. |
| **Only on the Composite class** (safety) | Type-safe -- you cannot call `add` on a leaf. The compiler enforces the distinction. | Clients that need to add children must know they have a Composite, which partially defeats the purpose of the uniform interface. |

The example above uses the **safety** approach: `add` and `remove` exist only on `Bundle`. This is generally the better default in Java because it catches misuse at compile time. Reserve the transparency approach for cases where the client truly must not know the difference.

### Overgeneralisation Risks

- **Bloated Component interface.** As the system evolves, there is a temptation to add every possible operation to the shared interface. This violates the Interface Segregation Principle and forces all leaves to implement methods that are irrelevant to them.
- **Implicit coupling to tree shape.** Some operations only make sense at certain depths (e.g., "apply coupon code" might only make sense on the root order, not on an individual product). The Composite pattern does not naturally express depth-dependent constraints.
- **Difficulty enforcing structural rules.** If your tree has constraints (e.g., "a bundle must contain at least two items", "nesting cannot exceed three levels"), the basic Composite pattern provides no mechanism to enforce them. You will need additional validation logic.

### Related Patterns

- **Iterator** is often used with Composite to traverse the tree without exposing its internal structure.
- **Visitor** pairs well with Composite when you need to define many unrelated operations over the tree without polluting the Component interface.
- **Decorator** also uses recursive composition but for a different purpose: wrapping behaviour around a single object rather than modelling a part-whole hierarchy.
- **Builder** can be useful for constructing complex Composite trees step by step.
