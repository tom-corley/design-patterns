# Template Method Pattern

**Classification:** Behavioural (GoF)

---

## 1. Core Idea

The Template Method pattern defines the **skeleton of an algorithm** in a base class method, deferring some steps to subclasses. Subclasses can override specific steps of the algorithm without changing its overall structure.

### The Problem

You have several classes that perform similar multi-step processes, but each class implements the full process independently. The high-level flow is identical -- read some data, transform it, write the output -- but the details of each step vary. This leads to two problems:

- **Duplicated control flow.** The orchestration logic (ordering of steps, error handling around those steps, logging, resource cleanup) is copy-pasted across every variant. When the shared logic needs to change, you must hunt down and update every copy.
- **No enforced contract.** There is no structural guarantee that each variant follows the same sequence of steps. One implementation might forget a validation step, another might reorder steps in a subtly incorrect way. The consistency of the algorithm is implicit and fragile.

### The Key Insight

Pull the invariant parts of the algorithm into a single method on an abstract base class -- the **template method**. This method calls a sequence of steps, some of which are abstract (subclasses *must* provide them) and some of which are concrete hooks with default behaviour (subclasses *may* override them). The template method itself is typically `final`, preventing subclasses from altering the overall flow.

The result is an **inversion of control** often called the "Hollywood Principle": *don't call us, we'll call you.* The base class drives execution; the subclass just fills in the blanks. This is the same principle that frameworks rely on -- you do not call the framework, the framework calls your code at the right moments.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **AbstractClass** | Declares the template method (usually `final`) that defines the algorithm skeleton. Declares abstract methods for steps that must vary, and may provide concrete hook methods with default (often empty) behaviour. |
| **ConcreteClass** | Implements all abstract methods declared by the AbstractClass. May optionally override hook methods to inject additional behaviour at predefined points in the algorithm. |

### How They Interact

```
      AbstractClass
      ┌──────────────────────────────┐
      │ + templateMethod() [final]   │  ← defines the algorithm skeleton
      │   calls:                     │
      │     step1()                  │  ← concrete (shared logic)
      │     step2()                  │  ← abstract (subclass must provide)
      │     step3()                  │  ← abstract (subclass must provide)
      │     hook()                   │  ← concrete with default (subclass may override)
      │                              │
      │ # step1()                    │
      │ # step2()          abstract  │
      │ # step3()          abstract  │
      │ # hook()           default   │
      └──────────┬───────────────────┘
                 │ extends
      ┌──────────┴───────────────────┐
      │       ConcreteClass          │
      │ # step2()  ← provides impl  │
      │ # step3()  ← provides impl  │
      │ # hook()   ← optional override│
      └──────────────────────────────┘
```

There are three kinds of methods in the abstract class:

1. **Template method** -- the `final` method that orchestrates the algorithm. Clients call this; subclasses never override it.
2. **Abstract (primitive) operations** -- steps that have no sensible default and *must* be provided by every subclass. These are the mandatory extension points.
3. **Hook methods** -- steps with a default implementation (often a no-op). Subclasses *may* override these to inject optional behaviour. Hooks give subclasses a way to "hook into" the algorithm at specific points without being forced to.

The distinction between abstract operations and hooks is important. Abstract operations are obligations; hooks are options. A well-designed template method makes this distinction clear through naming and documentation.

---

## 3. Use Cases

### Frameworks with Lifecycle Hooks

This is the most pervasive application of Template Method. Framework base classes define a lifecycle (initialise, configure, run, tear down) and call hook methods at each stage. Your application subclass overrides only the hooks it cares about. Examples include:

- **Servlet lifecycle** -- `HttpServlet` defines `service()` as the template method, which dispatches to `doGet()`, `doPost()`, etc. You override the HTTP-method-specific hooks.
- **Spring's `AbstractController`** -- the `handleRequest()` method manages session handling, caching headers, and response preparation, then delegates to your `handleRequestInternal()` override.
- **Android `Activity`** -- the system calls `onCreate()`, `onStart()`, `onResume()` in a fixed order. You override the hooks to set up your UI and resources.

### Data Processing Pipelines

When you have a standard extract-transform-load shape but the specifics of extraction and transformation differ per data source. The template method ensures that every pipeline variant opens a connection, reads data, transforms it, writes the output, and closes the connection -- in that order, with proper error handling -- even though the read/transform/write implementations differ completely.

### Build Systems and Code Generators

A build process follows a fixed sequence: resolve dependencies, compile sources, run tests, package artefacts. Each project type (Java, TypeScript, Go) provides different implementations of these steps but shares the orchestration logic.

### Test Frameworks (setUp / tearDown)

JUnit and TestNG use Template Method. The test runner calls `setUp()`, then the test method, then `tearDown()`, in that order. Your test class overrides these hooks to manage fixtures. The runner guarantees the ordering and ensures tearDown runs even if the test fails.

### Game Loop / Simulation Step

Game engines often define an `update()` template method that calls `handleInput()`, `updatePhysics()`, `render()` in sequence. Each game or scene subclass provides its own rendering and physics logic while the loop timing, frame-rate control, and input polling remain in the base class.

---

## 4. Example in Java

This example models a **data export pipeline**. The abstract class defines the overall flow: open a data source, extract records, transform each record, write the output, and close resources. Concrete subclasses implement exports to CSV and JSON formats.

### Abstract Class: `DataExporter`

```java
package behavioural.templatemethod;

import java.util.List;

/**
 * Defines the skeleton of a data export operation. Subclasses provide
 * format-specific extraction, transformation, and writing logic.
 */
public abstract class DataExporter {

    /**
     * The template method. Defines the fixed sequence of steps for any export.
     * Marked final so subclasses cannot alter the algorithm's structure.
     */
    public final void export() {
        openSource();
        List<String[]> rawRecords = extractRecords();

        System.out.printf("[%s] Extracted %d records.%n", formatName(), rawRecords.size());

        beforeTransform(rawRecords);

        List<String> transformed = rawRecords.stream()
                .map(this::transformRecord)
                .toList();

        String output = assembleOutput(transformed);
        writeOutput(output);
        closeSource();

        afterExport();

        System.out.printf("[%s] Export complete.%n", formatName());
    }

    // ----- Abstract (primitive) operations -- subclasses MUST implement -----

    /** Return a short name for the export format (e.g. "CSV", "JSON"). */
    protected abstract String formatName();

    /** Open the data source (file, database connection, API client, etc.). */
    protected abstract void openSource();

    /** Read raw records from the data source. Each record is a String array of field values. */
    protected abstract List<String[]> extractRecords();

    /** Transform a single record into its output representation. */
    protected abstract String transformRecord(String[] record);

    /** Assemble all transformed records into the final output document. */
    protected abstract String assembleOutput(List<String> transformedRecords);

    /** Write the assembled output to its destination. */
    protected abstract void writeOutput(String output);

    /** Close any resources opened by openSource(). */
    protected abstract void closeSource();

    // ----- Hook methods -- subclasses MAY override -----

    /**
     * Called after extraction but before transformation begins.
     * Default implementation does nothing. Override to add validation,
     * filtering, or logging of the raw data.
     */
    protected void beforeTransform(List<String[]> rawRecords) {
        // hook -- default is no-op
    }

    /**
     * Called after the export has completed successfully.
     * Default implementation does nothing. Override to send notifications,
     * update audit logs, or trigger downstream processes.
     */
    protected void afterExport() {
        // hook -- default is no-op
    }
}
```

### Concrete Class: `CsvExporter`

```java
package behavioural.templatemethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Exports data as a CSV document. Uses a hardcoded in-memory data source
 * for simplicity -- in production this would read from a database or file.
 */
public class CsvExporter extends DataExporter {

    private static final String[] HEADERS = {"id", "name", "email", "role"};

    private List<String[]> dataStore;

    @Override
    protected String formatName() {
        return "CSV";
    }

    @Override
    protected void openSource() {
        // Simulate opening a data source
        dataStore = new ArrayList<>();
        dataStore.add(new String[]{"1", "Alice Martin", "alice@example.com", "Engineer"});
        dataStore.add(new String[]{"2", "Bob Chen", "bob@example.com", "Designer"});
        dataStore.add(new String[]{"3", "Carol Reyes", "carol@example.com", "PM"});
        System.out.println("[CSV] Data source opened.");
    }

    @Override
    protected List<String[]> extractRecords() {
        return new ArrayList<>(dataStore);
    }

    @Override
    protected String transformRecord(String[] record) {
        return String.join(",", record);
    }

    @Override
    protected String assembleOutput(List<String> transformedRecords) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (String row : transformedRecords) {
            sb.append(row).append("\n");
        }
        return sb.toString();
    }

    @Override
    protected void writeOutput(String output) {
        System.out.println("[CSV] --- Output ---");
        System.out.print(output);
        System.out.println("[CSV] --- End ---");
    }

    @Override
    protected void closeSource() {
        dataStore = null;
        System.out.println("[CSV] Data source closed.");
    }
}
```

### Concrete Class: `JsonExporter`

```java
package behavioural.templatemethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Exports data as a JSON array. Demonstrates a different format while
 * sharing the same algorithm skeleton defined in DataExporter.
 */
public class JsonExporter extends DataExporter {

    private static final String[] FIELD_NAMES = {"id", "name", "email", "role"};

    private List<String[]> dataStore;

    @Override
    protected String formatName() {
        return "JSON";
    }

    @Override
    protected void openSource() {
        dataStore = new ArrayList<>();
        dataStore.add(new String[]{"1", "Alice Martin", "alice@example.com", "Engineer"});
        dataStore.add(new String[]{"2", "Bob Chen", "bob@example.com", "Designer"});
        dataStore.add(new String[]{"3", "Carol Reyes", "carol@example.com", "PM"});
        System.out.println("[JSON] Data source opened.");
    }

    @Override
    protected List<String[]> extractRecords() {
        return new ArrayList<>(dataStore);
    }

    @Override
    protected String transformRecord(String[] record) {
        StringBuilder sb = new StringBuilder("  {");
        for (int i = 0; i < FIELD_NAMES.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(FIELD_NAMES[i]).append("\": ");
            sb.append("\"").append(record[i]).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    protected String assembleOutput(List<String> transformedRecords) {
        return "[\n" + String.join(",\n", transformedRecords) + "\n]";
    }

    @Override
    protected void writeOutput(String output) {
        System.out.println("[JSON] --- Output ---");
        System.out.println(output);
        System.out.println("[JSON] --- End ---");
    }

    @Override
    protected void closeSource() {
        dataStore = null;
        System.out.println("[JSON] Data source closed.");
    }

    // Override a hook to add validation
    @Override
    protected void beforeTransform(List<String[]> rawRecords) {
        for (String[] record : rawRecords) {
            if (record.length != FIELD_NAMES.length) {
                throw new IllegalStateException(
                    "Record has " + record.length + " fields, expected " + FIELD_NAMES.length
                );
            }
        }
        System.out.println("[JSON] All records validated: field count OK.");
    }

    // Override a hook to simulate a notification
    @Override
    protected void afterExport() {
        System.out.println("[JSON] Notification sent: export available for download.");
    }
}
```

### Client: `ExportApp`

```java
package behavioural.templatemethod;

public class ExportApp {

    public static void main(String[] args) {

        System.out.println("========== CSV Export ==========");
        DataExporter csvExporter = new CsvExporter();
        csvExporter.export();

        System.out.println();

        System.out.println("========== JSON Export ==========");
        DataExporter jsonExporter = new JsonExporter();
        jsonExporter.export();
    }
}
```

### Expected Output

```
========== CSV Export ==========
[CSV] Data source opened.
[CSV] Extracted 3 records.
[CSV] --- Output ---
id,name,email,role
1,Alice Martin,alice@example.com,Engineer
2,Bob Chen,bob@example.com,Designer
3,Carol Reyes,carol@example.com,PM
[CSV] --- End ---
[CSV] Data source closed.
[CSV] Export complete.

========== JSON Export ==========
[JSON] Data source opened.
[JSON] Extracted 3 records.
[JSON] All records validated: field count OK.
[JSON] --- Output ---
[
  {"id": "1", "name": "Alice Martin", "email": "alice@example.com", "role": "Engineer"},
  {"id": "2", "name": "Bob Chen", "email": "bob@example.com", "role": "Designer"},
  {"id": "3", "name": "Carol Reyes", "email": "carol@example.com", "role": "PM"}
]
[JSON] --- End ---
[JSON] Data source closed.
[JSON] Notification sent: export available for download.
[JSON] Export complete.
```

### What to Notice

- **The algorithm sequence is identical** for both exports: open, extract, (hook), transform, assemble, write, close, (hook). Neither subclass can reorder or skip steps.
- **`CsvExporter` does not override any hooks** -- it relies on the default no-op behaviour. `JsonExporter` overrides both hooks to add validation and notification. Hooks are truly optional.
- **Adding a new format** (XML, Parquet, YAML) requires creating a new subclass and implementing the abstract methods. No existing code changes. This is the Open/Closed Principle at work.
- **The `final` modifier on `export()`** prevents a subclass from accidentally or deliberately breaking the algorithm structure. This is the pattern's core enforcement mechanism.

---

## 5. Tradeoffs and Limitations

### When to Use It

- **You have multiple classes that follow the same algorithm** but differ in specific steps. The shared structure is non-trivial (not just one or two lines), making the duplication worth eliminating.
- **You want to enforce an invariant sequence of operations.** The `final` template method guarantees that subclasses cannot skip validation, forget cleanup, or reorder steps.
- **You are building a framework or library** where users extend your base class. Template Method is the natural pattern for framework lifecycle hooks: the framework controls the flow, the user plugs in behaviour.
- **Hook methods provide useful extension points** without requiring every subclass to implement every step. The abstract/hook distinction lets you separate mandatory customisation from optional customisation.

### When Not to Use It

- **When the algorithm has only one or two steps.** The overhead of an abstract class, a template method, and abstract step methods is not justified if the shared structure is trivial. A simple interface with a default method may be clearer.
- **When the variations are not step-level but strategy-level.** If the entire behaviour (not just individual steps within a fixed sequence) varies between implementations, Strategy is a better fit. Template Method assumes a shared algorithm structure; Strategy assumes interchangeable algorithms.
- **When you need to compose behaviours at runtime.** Template Method uses inheritance, which is fixed at compile time. If you need to mix and match steps dynamically (e.g. "use CSV reading with JSON writing"), you need composition-based patterns like Strategy or a pipeline of function objects.

### Inheritance Coupling

The most significant limitation of Template Method is its reliance on inheritance. The subclass is tightly coupled to the base class:

- **Fragile base class problem.** Changes to the abstract class (adding a new step, changing the order, altering a hook's default behaviour) can break all subclasses. This is especially dangerous when the base class is in a library and the subclasses are in client code.
- **Single inheritance constraint.** In Java, a class can extend only one other class. If your class already extends something, it cannot also extend the template method's abstract class. This is a hard architectural constraint.
- **Deep hierarchies.** If you layer Template Method on top of Template Method (the abstract class itself extends another abstract class with its own template method), the resulting inheritance chain becomes difficult to reason about.

### Template Method vs Strategy

These two patterns solve related problems but make fundamentally different design choices:

| | Template Method | Strategy |
|---|---|---|
| **Mechanism** | Inheritance (subclass overrides steps) | Composition (inject a strategy object) |
| **Granularity** | Fine-grained -- varies individual steps within a fixed algorithm | Coarse-grained -- varies the entire algorithm |
| **Binding time** | Compile time (class hierarchy is fixed) | Runtime (strategies can be swapped) |
| **Number of classes** | One subclass per variant | One strategy object per variant, plus the context class |
| **Shared structure** | Enforced by the base class | Must be duplicated or extracted separately |
| **Flexibility** | Lower -- locked into the base class's skeleton | Higher -- mix and match freely |

A common refactoring path is to start with Template Method when you have a clear shared algorithm, then migrate to Strategy when you discover that the variations need to be composed or swapped at runtime.

### Liskov Substitution Concerns

The Liskov Substitution Principle (LSP) requires that subclass instances can be used wherever the base class is expected without altering the program's correctness. Template Method is generally LSP-friendly because the base class controls the algorithm structure and the subclass only fills in well-defined extension points. However, violations can occur:

- **Hook methods that change invariants.** If a hook override introduces side effects that the base class (or other clients) do not expect -- such as modifying shared state, throwing unexpected exceptions, or silently skipping a step -- it violates the behavioural contract.
- **Over-broad protected access.** If the abstract class exposes internal state through protected fields and a subclass mutates that state in unexpected ways, the template method's assumptions can be invalidated.

The mitigation is to keep the contract of each abstract/hook method well-documented, make the template method `final`, and minimise the protected surface area. Prefer passing data as method arguments over exposing mutable protected fields.

### Practical Tips

- **Use `final` on the template method.** This is not optional -- it is the pattern's enforcement mechanism. Without it, a subclass can override the entire algorithm, defeating the purpose.
- **Name hooks clearly.** Methods like `beforeTransform()`, `afterExport()`, and `onError()` signal their role in the lifecycle. Avoid generic names like `doStuff()`.
- **Keep the number of abstract methods small.** If a subclass must implement ten abstract methods to use your base class, the pattern is being over-applied. Three to five mandatory steps is a practical ceiling.
- **Document the call order.** Javadoc on the template method should list the steps in order so that someone reading a subclass can understand where their code fits in the larger flow without reading the base class source.
- **Consider combining with Strategy.** You can use Template Method for the overall algorithm skeleton and inject Strategy objects for individual steps that need runtime flexibility. This gives you the enforced structure of Template Method with the composability of Strategy where you need it.
