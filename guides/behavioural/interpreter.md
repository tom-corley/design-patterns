# Interpreter Pattern

**Classification:** Behavioural (GoF)

---

## 1. Core Idea

The Interpreter pattern defines a representation for a grammar of a given language together with an interpreter that uses that representation to interpret sentences in the language.

### The Problem

You have a recurring problem that can be expressed as sentences in a simple language. Parsing and evaluating those sentences with ad-hoc `if/else` or `switch` chains produces rigid code that is difficult to extend. Every new expression type or grammatical rule forces changes across multiple locations.

### The Key Insight

If you model each grammar rule as a class and compose them into a tree (an abstract syntax tree), you can evaluate any sentence by recursively walking that tree. Each node knows how to interpret itself given a shared context. Adding a new rule is simply adding a new class -- existing rules remain untouched.

The pattern effectively turns **data** (a sentence in the language) into **behaviour** (an object tree that can evaluate itself).

### When It Clicks

Think of a boolean expression like `(x AND y) OR (NOT z)`. Rather than writing a monolithic evaluator, you build a tree:

```
        OR
       /  \
     AND   NOT
    / \      \
   x   y      z
```

Each node is an `Expression` object. To evaluate, you call `interpret(context)` on the root and it delegates down. To add `XOR` support, you create one new class. Nothing else changes.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **AbstractExpression** | Declares an `interpret(Context)` method common to all nodes in the AST. |
| **TerminalExpression** | Implements `interpret` for terminal symbols in the grammar (leaf nodes -- variables, literals). |
| **NonterminalExpression** | Implements `interpret` for grammar rules that contain other expressions (composite nodes -- AND, OR, NOT). Each nonterminal holds references to its child expressions. |
| **Context** | Contains the global information the interpreter needs -- typically variable bindings, input state, or an environment map. |
| **Client** | Builds (or receives) the abstract syntax tree and invokes `interpret` on the root. |

### Class Diagram (Textual)

```
         <<interface>>
        AbstractExpression
      + interpret(ctx: Context): T
              ^
              |
     -------------------------
     |                       |
TerminalExpression    NonterminalExpression
+ interpret(ctx)      - left: AbstractExpression
                      - right: AbstractExpression
                      + interpret(ctx)
```

### How They Relate

1. The **Client** constructs the AST. In practice this is done by a parser, but the Interpreter pattern itself is concerned with the evaluation side, not the parsing side.
2. Each **NonterminalExpression** delegates to its children, combining their results according to the grammar rule it represents.
3. Each **TerminalExpression** looks up its value in the **Context** (e.g. resolving a variable name to a boolean value).
4. The **Context** is threaded through every `interpret` call, providing the external state needed for evaluation.

---

## 3. Use Cases

### Boolean / Logical Rule Engines

Business rule engines often let users define conditions in a simple boolean language: `"userAge >= 18 AND hasConsent"`. Each condition becomes an expression node; the engine evaluates the tree against a fact context.

### Mathematical Expression Evaluation

Calculator applications, spreadsheet formula engines, and financial modelling tools parse arithmetic expressions (`(price * quantity) - discount`) into an AST and evaluate them with an Interpreter.

### SQL WHERE Clause Parsing

A simplified query engine can model `WHERE` clauses as an expression tree. `column = value AND column > value` becomes a tree of comparison and logical nodes interpreted against each row.

### Regular Expression Engines

Regex implementations internally compile patterns into an expression tree (or NFA). Conceptually, each regex construct -- literal, alternation, repetition, grouping -- is an expression node that interprets whether a given input string matches.

### Domain-Specific Languages (DSLs)

Configuration languages, template engines, and workflow definition languages all fit the Interpreter model. If your DSL grammar is small (say, under 20 rules), the pattern provides a clean, extensible evaluation mechanism.

### Robot / Turtle Command Languages

Educational programming environments (Logo, Scratch-like tools) parse command sequences like `FORWARD 10 TURN LEFT FORWARD 5` into expression trees and interpret them to produce actions.

---

## 4. Example in Java

The following example implements a **boolean expression evaluator**. It supports variables, the literal values `TRUE` and `FALSE`, and the operators `AND`, `OR`, and `NOT`. The grammar:

```
expression ::= variable | 'TRUE' | 'FALSE'
             | expression 'AND' expression
             | expression 'OR' expression
             | 'NOT' expression
```

### AbstractExpression

```java
/**
 * The root of the expression hierarchy.
 * Every node in the AST can interpret itself given a context.
 */
public interface Expression {
    boolean interpret(Context context);
}
```

### Context

```java
import java.util.HashMap;
import java.util.Map;

/**
 * Holds variable bindings for the boolean language.
 */
public class Context {

    private final Map<String, Boolean> variables = new HashMap<>();

    public void assign(String name, boolean value) {
        variables.put(name, value);
    }

    public boolean lookup(String name) {
        if (!variables.containsKey(name)) {
            throw new IllegalArgumentException("Undefined variable: " + name);
        }
        return variables.get(name);
    }
}
```

### TerminalExpression -- Variable

```java
/**
 * A terminal expression that resolves a named variable from the context.
 */
public class VariableExpression implements Expression {

    private final String name;

    public VariableExpression(String name) {
        this.name = name;
    }

    @Override
    public boolean interpret(Context context) {
        return context.lookup(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
```

### TerminalExpression -- Constant

```java
/**
 * A terminal expression for literal boolean values (TRUE / FALSE).
 */
public class ConstantExpression implements Expression {

    private final boolean value;

    public ConstantExpression(boolean value) {
        this.value = value;
    }

    @Override
    public boolean interpret(Context context) {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value).toUpperCase();
    }
}
```

### NonterminalExpression -- AND

```java
/**
 * A nonterminal expression representing logical AND.
 */
public class AndExpression implements Expression {

    private final Expression left;
    private final Expression right;

    public AndExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean interpret(Context context) {
        return left.interpret(context) && right.interpret(context);
    }

    @Override
    public String toString() {
        return "(" + left + " AND " + right + ")";
    }
}
```

### NonterminalExpression -- OR

```java
/**
 * A nonterminal expression representing logical OR.
 */
public class OrExpression implements Expression {

    private final Expression left;
    private final Expression right;

    public OrExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean interpret(Context context) {
        return left.interpret(context) || right.interpret(context);
    }

    @Override
    public String toString() {
        return "(" + left + " OR " + right + ")";
    }
}
```

### NonterminalExpression -- NOT

```java
/**
 * A nonterminal expression representing logical NOT (unary).
 */
public class NotExpression implements Expression {

    private final Expression operand;

    public NotExpression(Expression operand) {
        this.operand = operand;
    }

    @Override
    public boolean interpret(Context context) {
        return !operand.interpret(context);
    }

    @Override
    public String toString() {
        return "(NOT " + operand + ")";
    }
}
```

### Client -- Building and Evaluating the AST

```java
/**
 * Demonstrates building an expression tree by hand and interpreting it.
 *
 * Expression: (x AND y) OR (NOT z)
 *
 * With x=true, y=false, z=true  -->  (true AND false) OR (NOT true)  -->  false OR false  -->  false
 * With x=true, y=true,  z=false -->  (true AND true)  OR (NOT false) -->  true  OR true   -->  true
 */
public class InterpreterDemo {

    public static void main(String[] args) {

        // Build the AST for: (x AND y) OR (NOT z)
        Expression x = new VariableExpression("x");
        Expression y = new VariableExpression("y");
        Expression z = new VariableExpression("z");

        Expression xAndY = new AndExpression(x, y);
        Expression notZ  = new NotExpression(z);
        Expression root  = new OrExpression(xAndY, notZ);

        System.out.println("Expression: " + root);

        // --- First evaluation ---
        Context ctx1 = new Context();
        ctx1.assign("x", true);
        ctx1.assign("y", false);
        ctx1.assign("z", true);

        boolean result1 = root.interpret(ctx1);
        System.out.println("x=true, y=false, z=true  => " + result1);  // false

        // --- Second evaluation with different bindings ---
        Context ctx2 = new Context();
        ctx2.assign("x", true);
        ctx2.assign("y", true);
        ctx2.assign("z", false);

        boolean result2 = root.interpret(ctx2);
        System.out.println("x=true, y=true,  z=false => " + result2);  // true

        // --- Demonstrate extensibility: add a constant ---
        Expression alwaysTrue = new OrExpression(
            new ConstantExpression(true),
            new VariableExpression("x")
        );
        System.out.println("\n" + alwaysTrue + " => " + alwaysTrue.interpret(ctx1));  // true
    }
}
```

### Expected Output

```
Expression: ((x AND y) OR (NOT z))
x=true, y=false, z=true  => false
x=true, y=true,  z=false => true

(TRUE OR x) => true
```

### Extending the Language

To add an `XOR` operator, you write a single new class:

```java
public class XorExpression implements Expression {

    private final Expression left;
    private final Expression right;

    public XorExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean interpret(Context context) {
        return left.interpret(context) ^ right.interpret(context);
    }

    @Override
    public String toString() {
        return "(" + left + " XOR " + right + ")";
    }
}
```

No existing expression classes need to change. This is the pattern's central strength.

---

## 5. Tradeoffs and Limitations

### Advantages

| Advantage | Detail |
|---|---|
| **Easy to extend the grammar** | Adding a new expression type means adding one class. Existing types are untouched (Open/Closed Principle). |
| **Grammar maps directly to classes** | Each rule in the BNF grammar corresponds to a class, making the design self-documenting and easy to reason about. |
| **Reusable across contexts** | The same expression tree can be evaluated against different contexts (different variable bindings, different environments). |
| **Composable** | Expressions naturally compose into arbitrarily deep trees. Complex sentences are built from simple parts. |

### Disadvantages

| Disadvantage | Detail |
|---|---|
| **Class explosion for complex grammars** | Each grammar rule requires its own class. A grammar with 30+ rules produces 30+ classes, which becomes unwieldy. |
| **Performance** | Recursive tree-walking is slow for large expressions or high-frequency evaluation. There is no optimisation, caching, or compilation step unless you add one yourself. |
| **Not suitable for complex languages** | If the grammar has ambiguity, precedence rules, error recovery, or context-sensitive features, the Interpreter pattern alone is not enough. |
| **Parsing is a separate problem** | The pattern addresses evaluation, not parsing. You still need to build the AST somehow -- by hand, with a recursive descent parser, or with a parser generator. |

### When to Use

- The language is **simple** -- a small number of grammar rules (roughly under 15-20).
- The grammar is **stable** and changes infrequently.
- **Efficiency is not critical** -- the expressions are evaluated occasionally, not millions of times per second.
- You need the ability to **swap contexts** easily (evaluate the same expression against different data).

### When to Avoid

- The grammar is **large or complex**. At that point, use a **parser generator** (ANTLR, JavaCC, PEG parsers) that produces optimised parsing and evaluation code.
- You need **high performance**. Consider compiling the AST into bytecode, or using the Visitor pattern to separate evaluation strategies from the tree structure.
- The language has **syntactic ambiguity** or requires sophisticated error handling. The Interpreter pattern provides no built-in support for these concerns.

### Alternatives and Complements

| Alternative | When to Prefer |
|---|---|
| **Visitor pattern** | When you have a fixed set of expression types but need many different operations over the tree (evaluation, pretty-printing, optimisation, type-checking). Visitor separates the operations from the node classes. |
| **Parser generators (ANTLR, JavaCC)** | When the grammar is non-trivial. These tools generate efficient parsers from a grammar specification and can also generate AST node classes. |
| **Composite pattern** | The Interpreter pattern is structurally a specialisation of Composite. If your "interpretation" is simple aggregation, Composite alone may suffice. |
| **Strategy pattern** | If you only have a small, flat set of "expressions" (no recursive nesting), Strategy may be a simpler fit. |
| **Compiled / bytecode approach** | For performance-critical evaluation, compile the AST into an intermediate representation or bytecode rather than interpreting the tree directly. |

The Interpreter pattern gives you a clean, extensible way to evaluate sentences in a simple language by mapping grammar rules to an object hierarchy. Its strength lies in small, stable grammars where adding new expression types should not require modifying existing code. For anything beyond a simple DSL, pair it with a proper parser on the front end, or reach for a parser generator that handles both parsing and evaluation at scale.
