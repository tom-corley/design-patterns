# Visitor Pattern

**Classification:** Behavioural (GoF)

---

## 1. Core Idea

The Visitor pattern lets you define new operations on an object structure without modifying the classes of the elements in that structure. It separates algorithms from the objects they operate on.

### The Problem

You have a collection of objects with different types -- say, an abstract syntax tree with `NumberLiteral`, `BinaryExpression`, and `FunctionCall` nodes -- and you need to perform many different operations across them: type-checking, code generation, pretty-printing, optimisation passes. The naive approach is to add a method for each operation to every node class. This scatters unrelated concerns across the hierarchy and forces you to modify every element class each time you invent a new operation. If the element classes live in a library you do not control, you cannot modify them at all.

### The Key Insight: Double Dispatch

Most object-oriented languages support **single dispatch**: the method that gets called depends on the runtime type of the receiver (`element.doSomething()`). But here you need the behaviour to depend on *two* runtime types -- the type of the element *and* the type of the operation. This is **double dispatch**, and most languages do not support it natively.

The Visitor pattern simulates double dispatch with two method calls:

1. You call `element.accept(visitor)` -- this dispatches on the element's runtime type.
2. Inside `accept`, the element calls `visitor.visitConcreteElement(this)` -- this dispatches on the visitor's runtime type.

After these two calls, the runtime knows both the concrete element type and the concrete visitor type, so the correct operation runs. Each new operation is a new Visitor class. The element hierarchy does not change.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Visitor** | An interface declaring a `visit` method for each ConcreteElement type in the hierarchy. |
| **ConcreteVisitor** | Implements the Visitor interface, providing the actual logic for each element type. Each ConcreteVisitor represents one operation. |
| **Element** | An interface declaring an `accept(Visitor)` method. |
| **ConcreteElement** | Implements `accept` by calling the appropriate `visit` method on the visitor, passing `this`. |
| **ObjectStructure** | The collection or composite that holds Elements and lets a Visitor traverse them. |

### How Double Dispatch Works

```
Client code:
    for each element in objectStructure:
        element.accept(visitor)

Inside ConcreteElementA.accept(Visitor v):
    v.visitConcreteElementA(this)    // <-- compiler knows `this` is ConcreteElementA

Inside ConcreteVisitor.visitConcreteElementA(ConcreteElementA e):
    // Now we know BOTH the element type (A) and the visitor type (ConcreteVisitor)
    // -- full double dispatch achieved.
```

The first call (`accept`) resolves the element type via polymorphism. The second call (`visitX`) resolves the visitor type via polymorphism. Two virtual dispatches, hence "double dispatch."

### Conceptual Diagram

```
    <<interface>>          <<interface>>
       Element                Visitor
    + accept(Visitor)      + visitA(ConcreteElementA)
         |                 + visitB(ConcreteElementB)
        / \                       |
       /   \                     / \
ConcreteA  ConcreteB    PrintVisitor  EvalVisitor
```

`ObjectStructure` holds a collection of `Element` references and iterates them, calling `accept` with the provided visitor.

---

## 3. Use Cases

### Compiler / AST Traversal

The textbook example. A compiler's abstract syntax tree has many node types (literals, binary operations, function calls, variable declarations, control flow). Operations over the tree -- type-checking, optimisation, code generation, pretty-printing -- are each implemented as a separate Visitor. Adding a new compiler pass means writing a new Visitor class; no AST node classes change.

### Document Export

A document object model contains paragraphs, headings, images, tables, and code blocks. Exporting to HTML, Markdown, PDF, and LaTeX are four different operations. Each export format is a ConcreteVisitor that knows how to render every element type into its target format.

### Tax and Pricing Calculations

An e-commerce system has different product types: physical goods, digital downloads, subscriptions, gift cards. Tax rules differ by product type and by jurisdiction. Each jurisdiction's tax calculator is a Visitor that implements the correct tax logic for every product type. Adding a new jurisdiction means adding a new Visitor, not touching any product class.

### Report Generation

A financial system has different instrument types: equities, bonds, options, futures. Generating a risk report, a P&L report, and a regulatory report are three different traversals over the same portfolio. Each report type is a Visitor.

### Static Analysis and Linting

A linter walks a code AST to find issues: unused variables, unreachable code, style violations. Each lint rule can be its own Visitor, making rules independently addable and removable.

---

## 4. Example in Java

This example models an expression tree (AST) with three node types, and two visitors: one that pretty-prints the expression and one that evaluates it.

### The Element Interface

```java
public interface Expression {

    /**
     * Accept a visitor -- the first half of double dispatch.
     */
    <R> R accept(ExpressionVisitor<R> visitor);
}
```

### Concrete Elements

```java
public record NumberLiteral(double value) implements Expression {

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitNumber(this);
    }
}
```

```java
public record BinaryExpression(Expression left, Operator operator, Expression right) implements Expression {

    public enum Operator {
        ADD("+"), SUBTRACT("-"), MULTIPLY("*"), DIVIDE("/");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String symbol() {
            return symbol;
        }
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitBinary(this);
    }
}
```

```java
public record Negate(Expression operand) implements Expression {

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitNegate(this);
    }
}
```

### The Visitor Interface

```java
public interface ExpressionVisitor<R> {

    R visitNumber(NumberLiteral number);

    R visitBinary(BinaryExpression binary);

    R visitNegate(Negate negate);
}
```

The generic type parameter `R` lets each visitor declare its own return type -- `Double` for evaluation, `String` for printing. This avoids accumulating results in mutable state.

### ConcreteVisitor: Evaluator

```java
public class Evaluator implements ExpressionVisitor<Double> {

    @Override
    public Double visitNumber(NumberLiteral number) {
        return number.value();
    }

    @Override
    public Double visitBinary(BinaryExpression binary) {
        double left = binary.left().accept(this);
        double right = binary.right().accept(this);

        return switch (binary.operator()) {
            case ADD      -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE   -> {
                if (right == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                yield left / right;
            }
        };
    }

    @Override
    public Double visitNegate(Negate negate) {
        return -negate.operand().accept(this);
    }
}
```

### ConcreteVisitor: Pretty Printer

```java
public class PrettyPrinter implements ExpressionVisitor<String> {

    @Override
    public String visitNumber(NumberLiteral number) {
        double value = number.value();
        // Print integers without the decimal point
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }

    @Override
    public String visitBinary(BinaryExpression binary) {
        String left = binary.left().accept(this);
        String right = binary.right().accept(this);
        return "(" + left + " " + binary.operator().symbol() + " " + right + ")";
    }

    @Override
    public String visitNegate(Negate negate) {
        String operand = negate.operand().accept(this);
        return "(-" + operand + ")";
    }
}
```

### ConcreteVisitor: Depth Calculator

A third visitor that computes the depth of the expression tree, demonstrating how easily new operations are added.

```java
public class DepthCalculator implements ExpressionVisitor<Integer> {

    @Override
    public Integer visitNumber(NumberLiteral number) {
        return 1;
    }

    @Override
    public Integer visitBinary(BinaryExpression binary) {
        int left = binary.left().accept(this);
        int right = binary.right().accept(this);
        return 1 + Math.max(left, right);
    }

    @Override
    public Integer visitNegate(Negate negate) {
        return 1 + negate.operand().accept(this);
    }
}
```

### Client Code

```java
import static BinaryExpression.Operator.*;

public class ExpressionDemo {

    public static void main(String[] args) {
        // Build the expression: (3 + 5) * -(2 / 4)
        Expression expr = new BinaryExpression(
            new BinaryExpression(
                new NumberLiteral(3),
                ADD,
                new NumberLiteral(5)
            ),
            MULTIPLY,
            new Negate(
                new BinaryExpression(
                    new NumberLiteral(2),
                    DIVIDE,
                    new NumberLiteral(4)
                )
            )
        );

        // Apply different visitors to the same structure
        PrettyPrinter printer = new PrettyPrinter();
        Evaluator evaluator = new Evaluator();
        DepthCalculator depthCalc = new DepthCalculator();

        System.out.println("Expression: " + expr.accept(printer));
        System.out.println("Result:     " + expr.accept(evaluator));
        System.out.println("Depth:      " + expr.accept(depthCalc));

        // A simpler expression: -(7 + 3)
        Expression simple = new Negate(
            new BinaryExpression(
                new NumberLiteral(7),
                ADD,
                new NumberLiteral(3)
            )
        );

        System.out.println();
        System.out.println("Expression: " + simple.accept(printer));
        System.out.println("Result:     " + simple.accept(evaluator));
        System.out.println("Depth:      " + simple.accept(depthCalc));
    }
}
```

### Expected Output

```
Expression: ((3 + 5) * (-(2 / 4)))
Result:     -4.0
Depth:      4

Expression: (-(7 + 3))
Result:     -10.0
Depth:      3
```

### Why This Works

Adding a new operation -- say, a `ConstantFolder` that simplifies constant sub-expressions at compile time -- requires writing one new class that implements `ExpressionVisitor<Expression>`. No existing element or visitor class changes. The element hierarchy is **closed for modification but open for extension via visitors**.

---

## 5. Tradeoffs and Limitations

### When to Use the Visitor Pattern

- **The element hierarchy is stable.** You rarely add new element types, but you frequently add new operations. Visitor makes new operations cheap (one new class) at the cost of making new element types expensive (every visitor must be updated).
- **You need multiple unrelated operations over a heterogeneous structure.** If you only need one operation, a simple polymorphic method on each element class is clearer.
- **You want to accumulate state across a traversal.** A visitor object can carry fields that accumulate results as it visits elements (e.g., summing values, collecting errors).
- **Operations should not pollute element classes.** Visitor keeps the element hierarchy focused on representing data, not performing operations on it.

### When Not to Use It

- **The element hierarchy changes frequently.** Every new ConcreteElement type forces a change to the Visitor interface and every ConcreteVisitor. If your hierarchy is volatile, Visitor creates a maintenance burden.
- **There is only one operation.** The pattern's indirection (double dispatch, separate visitor classes) is not justified for a single algorithm. Just put the method on the elements.
- **The elements need to hide their internals.** Visitor often requires elements to expose enough state for the visitor to do its work. This can break encapsulation -- the element must provide public accessors for fields that would otherwise be private.

### The Expression Problem

The Visitor pattern sits on one side of the **expression problem**, a well-known tension in language design:

|  | Adding new operations | Adding new data types |
|---|---|---|
| **Visitor pattern** | Easy -- new Visitor class | Hard -- change Visitor interface + all implementations |
| **Polymorphic methods on elements** | Hard -- change every element class | Easy -- new subclass with its own methods |

Neither approach makes both dimensions easy to extend. Visitor bets that operations change more often than data types. If that bet is wrong, the pattern fights you.

### Breaking Encapsulation

For a visitor to do useful work, it typically needs access to the element's internal data. In the AST example above, `Evaluator` needs to read `NumberLiteral.value()` and `BinaryExpression.operator()`. This is acceptable for value-like objects (especially Java records, which are transparent by design), but for elements with complex invariants, exposing internals for a visitor's benefit can erode encapsulation.

### Difficulty Adding New Element Types

If you add a `FunctionCall` node to the AST, you must:

1. Add `visitFunctionCall(FunctionCall call)` to the `ExpressionVisitor` interface.
2. Implement it in **every** existing ConcreteVisitor.

This is an all-or-nothing change. The compiler enforces it (which is actually a safety benefit -- you will not forget a case), but it can be a significant effort if there are many visitors.

### Sealed Classes and Pattern Matching in Modern Java

Java 17+ sealed classes and Java 21+ pattern matching with `switch` offer an alternative to the classic Visitor pattern for closed hierarchies:

```java
public sealed interface Expression
    permits NumberLiteral, BinaryExpression, Negate {}

public static double evaluate(Expression expr) {
    return switch (expr) {
        case NumberLiteral n    -> n.value();
        case BinaryExpression b -> {
            double left = evaluate(b.left());
            double right = evaluate(b.right());
            yield switch (b.operator()) {
                case ADD      -> left + right;
                case SUBTRACT -> left - right;
                case MULTIPLY -> left * right;
                case DIVIDE   -> left / right;
            };
        }
        case Negate neg -> -evaluate(neg.operand());
    };
}
```

This achieves the same goals as Visitor -- exhaustive handling of all element types, operations separated from data -- but without the `accept`/`visit` ceremony. The compiler still enforces exhaustiveness because the interface is `sealed`. If you add a new permitted type, every `switch` over the sealed interface becomes a compile error until updated.

**When to prefer sealed + pattern matching over Visitor:**

- The hierarchy is defined in the same codebase (so you can seal it).
- You value simpler code over strict adherence to the GoF structure.
- You are on Java 21+ and can use pattern matching switches.

**When classic Visitor still wins:**

- The element hierarchy is defined in a library you do not control (you cannot add `sealed`).
- You need visitors to carry mutable traversal state across elements.
- You want visitors to be pluggable at runtime (passed as objects, stored in collections, etc.).

### Practical Tips

- **Use a generic return type on the visitor interface** (as in the example's `ExpressionVisitor<R>`) to avoid casting and mutable accumulator fields.
- **Provide a default visitor.** An abstract base class that implements the visitor interface with default no-op or identity implementations lets concrete visitors override only the methods they care about. This is especially valuable when the element hierarchy is large.
- **Separate traversal from visitation.** In the AST example, each `visitBinary` call recursively visits its children. For more complex structures (like graphs with cycles), extract the traversal logic into the ObjectStructure or a dedicated iterator, and let the visitor handle only the per-node logic.
- **Consider the Acyclic Visitor variant** if you need to decouple visitors from specific element types (at the cost of runtime type checks instead of compile-time safety).
