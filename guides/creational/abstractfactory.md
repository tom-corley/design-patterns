# Abstract Factory Pattern

**Classification:** Creational (GoF)

---

## 1. Core Idea

The Abstract Factory pattern provides an interface for creating **families of related or dependent objects** without specifying their concrete classes.

### The Problem

Imagine you are building a UI toolkit that must run on multiple platforms — Windows, macOS, and Linux. Each platform has its own look-and-feel for buttons, checkboxes, text fields, and scrollbars. Your application code needs to create these widgets, but it should not be coupled to any specific platform implementation. If you scatter `new WindowsButton()` and `new MacCheckbox()` calls throughout your codebase, you end up with:

- **Platform-specific code leaking everywhere.** Every place that creates a widget must know about every platform variant.
- **Inconsistent families.** Nothing prevents you from accidentally mixing a Windows button with a macOS checkbox in the same dialog.
- **Painful extensibility.** Adding a new platform means hunting down every widget creation site.

### The Key Insight

Extract the creation of each product in a family behind a common factory interface. Each concrete factory is responsible for producing **all products in one family**, guaranteeing consistency. Client code programs against the abstract factory and abstract product interfaces, never touching concrete classes directly.

The result: you can swap entire product families by swapping a single factory object — and the compiler ensures you never mix products from different families.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **AbstractFactory** | Declares creation methods for each abstract product type (e.g. `createButton()`, `createCheckbox()`). |
| **ConcreteFactory** | Implements the creation methods to produce products belonging to one specific family (e.g. `WindowsWidgetFactory`, `MacWidgetFactory`). |
| **AbstractProduct** | Declares the interface for a type of product (e.g. `Button`, `Checkbox`). |
| **ConcreteProduct** | A platform- or family-specific implementation of an abstract product (e.g. `WindowsButton`, `MacCheckbox`). |
| **Client** | Uses only the AbstractFactory and AbstractProduct interfaces. Never references concrete classes. |

### Relationships

```
                    AbstractFactory
                   /               \
         createButton()        createCheckbox()
                |                     |
        --------+--------     --------+--------
        |                |    |                |
  ConcreteFactory1  ConcreteFactory2
  (WindowsWidgetFactory) (MacWidgetFactory)
        |                     |
  creates                creates
        |                     |
  WindowsButton          MacButton
  WindowsCheckbox        MacCheckbox


  AbstractProduct (Button)  <---  WindowsButton, MacButton
  AbstractProduct (Checkbox) <--- WindowsCheckbox, MacCheckbox
```

The client receives a factory (via constructor injection, configuration, or a bootstrap method) and calls its creation methods. It never knows — or needs to know — which concrete family it is working with.

---

## 3. Use Cases

### Cross-Platform UI Toolkits
The canonical example. Swing's `LookAndFeel`, SWT's platform delegates, and JavaFX's skin system all use variations of this pattern. You choose a platform factory at startup, and every widget produced is consistent with that platform.

### Database Access Layers
A `DatabaseFactory` interface with methods like `createConnection()`, `createStatement()`, and `createResultSetHandler()`. Concrete factories for PostgreSQL, MySQL, and SQLite each return driver-specific implementations that are guaranteed to work together.

### Document Export Systems
An `ExportFactory` with `createHeader()`, `createParagraph()`, `createTable()`. Concrete factories for PDF, HTML, and DOCX produce format-specific elements. The rendering engine works against the abstract types and never imports a PDF library directly.

### Game Asset Loading
A `ThemeFactory` that produces `Terrain`, `Unit`, and `Building` objects. A `MedievalThemeFactory` and a `SciFiThemeFactory` each produce a complete, visually consistent set of game assets.

### Cloud Provider Abstraction
A `CloudFactory` with `createComputeInstance()`, `createStorageService()`, `createMessageQueue()`. Concrete factories for AWS, GCP, and Azure ensure you never accidentally mix an S3 bucket with a GCP Pub/Sub topic in the same deployment module.

---

## 4. Example in Java

The example below models a **cross-platform UI toolkit** with two widget types (Button and Checkbox) and two platforms (Windows and macOS).

### Abstract Products

```java
/**
 * Abstract product: Button.
 * Every platform provides its own rendering of a button.
 */
public interface Button {
    void render();
    void onClick(Runnable handler);
}
```

```java
/**
 * Abstract product: Checkbox.
 * Every platform provides its own rendering of a checkbox.
 */
public interface Checkbox {
    void render();
    boolean isChecked();
    void toggle();
}
```

### Concrete Products — Windows Family

```java
public class WindowsButton implements Button {
    private Runnable clickHandler;

    @Override
    public void render() {
        System.out.println("[Windows] Rendering a flat, Metro-style button.");
    }

    @Override
    public void onClick(Runnable handler) {
        this.clickHandler = handler;
        System.out.println("[Windows] Click handler registered on Windows button.");
    }
}
```

```java
public class WindowsCheckbox implements Checkbox {
    private boolean checked = false;

    @Override
    public void render() {
        String state = checked ? "checked" : "unchecked";
        System.out.println("[Windows] Rendering a square Windows checkbox (" + state + ").");
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void toggle() {
        checked = !checked;
        System.out.println("[Windows] Checkbox toggled to " + checked + ".");
    }
}
```

### Concrete Products — macOS Family

```java
public class MacButton implements Button {
    private Runnable clickHandler;

    @Override
    public void render() {
        System.out.println("[macOS] Rendering a rounded Aqua-style button.");
    }

    @Override
    public void onClick(Runnable handler) {
        this.clickHandler = handler;
        System.out.println("[macOS] Click handler registered on macOS button.");
    }
}
```

```java
public class MacCheckbox implements Checkbox {
    private boolean checked = false;

    @Override
    public void render() {
        String state = checked ? "checked" : "unchecked";
        System.out.println("[macOS] Rendering a rounded macOS checkbox (" + state + ").");
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void toggle() {
        checked = !checked;
        System.out.println("[macOS] Checkbox toggled to " + checked + ".");
    }
}
```

### Abstract Factory

```java
/**
 * The abstract factory declares creation methods for each product type.
 * Concrete factories must produce a full family of products.
 */
public interface WidgetFactory {
    Button createButton();
    Checkbox createCheckbox();
}
```

### Concrete Factories

```java
public class WindowsWidgetFactory implements WidgetFactory {
    @Override
    public Button createButton() {
        return new WindowsButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new WindowsCheckbox();
    }
}
```

```java
public class MacWidgetFactory implements WidgetFactory {
    @Override
    public Button createButton() {
        return new MacButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new MacCheckbox();
    }
}
```

### Client Code

```java
/**
 * The Application class is the client. It depends only on the abstract
 * WidgetFactory and the abstract product interfaces (Button, Checkbox).
 * It has zero knowledge of Windows or macOS specifics.
 */
public class Application {
    private final Button button;
    private final Checkbox checkbox;

    public Application(WidgetFactory factory) {
        this.button = factory.createButton();
        this.checkbox = factory.createCheckbox();
    }

    public void renderUI() {
        button.render();
        checkbox.render();
    }

    public void interact() {
        button.onClick(() -> System.out.println("Button was clicked!"));
        checkbox.toggle();
        checkbox.render();
    }
}
```

### Bootstrap / Main

```java
/**
 * The bootstrap code is the ONLY place that references concrete factories.
 * In a real application this would be driven by configuration, OS detection,
 * or dependency injection.
 */
public class Main {
    public static void main(String[] args) {
        // Detect the platform (simulated here with a system property)
        String os = System.getProperty("os.name", "").toLowerCase();

        WidgetFactory factory;
        if (os.contains("mac")) {
            factory = new MacWidgetFactory();
        } else {
            factory = new WindowsWidgetFactory();
        }

        // From this point on, the application is platform-agnostic
        Application app = new Application(factory);
        app.renderUI();

        System.out.println();

        app.interact();
    }
}
```

### Sample Output (on macOS)

```
[macOS] Rendering a rounded Aqua-style button.
[macOS] Rendering a rounded macOS checkbox (unchecked).

[macOS] Click handler registered on macOS button.
[macOS] Checkbox toggled to true.
[macOS] Rendering a rounded macOS checkbox (checked).
```

### What to Notice

- **The `Application` class never imports a concrete product or factory.** It is fully decoupled from any platform.
- **Adding a new platform** (e.g. Linux) requires creating `LinuxButton`, `LinuxCheckbox`, and `LinuxWidgetFactory`. No existing client code changes.
- **Adding a new product type** (e.g. `TextField`) requires adding a method to `WidgetFactory` and implementing it in every concrete factory. This is the pattern's main extensibility cost.
- **Family consistency is enforced structurally.** A `WindowsWidgetFactory` can only produce Windows widgets. You cannot accidentally create a macOS checkbox alongside a Windows button.

---

## 5. Tradeoffs and Limitations

### When to Use It

- You need to produce **families of related objects** that must be used together.
- Client code should be independent of how products are created and composed.
- You want to enforce consistency within a product family at compile time.
- You expect the set of families to grow (new platforms, new themes, new providers) while the product types remain relatively stable.

### When Not to Use It

- **You only have one product type.** A regular Factory Method is simpler and sufficient. Abstract Factory is justified by the "family" dimension — if there is no family, the abstraction is overhead.
- **Product types change frequently.** Every new product type forces a change to the abstract factory interface and every concrete factory. If your product lineup is volatile, this pattern becomes a maintenance burden.
- **You only have one family.** If there is only one concrete factory and you do not anticipate more, you are paying the cost of the abstraction without the benefit of polymorphic substitution.
- **Simple construction logic.** If object creation is trivial (no configuration, no family constraints), a straightforward `new` call or a simple static factory method is easier to understand and maintain.

### Complexity Cost

| Dimension | Impact |
|---|---|
| **Class count** | High. Each product x family combination is a class, plus the factory interfaces. For P product types and F families, you get P x F concrete products + F concrete factories + P + 1 interfaces. |
| **Indirection** | Moderate. Readers must trace from the factory interface to the concrete factory to the concrete product to understand what is actually created. |
| **Rigidity on the product axis** | Adding a new product type is an invasive change: the abstract factory interface changes, and every concrete factory must be updated. This violates the Open/Closed Principle along the product dimension. |
| **Flexibility on the family axis** | Excellent. Adding a new family is purely additive: implement the factory interface and the product interfaces. No existing code changes. |

### Alternatives to Consider

- **Factory Method:** When you need polymorphic creation of a single product type rather than a family. Less structure, fewer classes.
- **Builder:** When the complexity is in the construction process (many optional parameters, step-by-step assembly) rather than in choosing between product families.
- **Prototype:** When product families can be represented as cloneable prototypical instances rather than separate class hierarchies.
- **Dependency Injection (DI) Containers:** In modern applications, a DI framework (Spring, Guice, Dagger) often replaces hand-written abstract factories. You configure bindings per profile/environment, and the container wires the correct implementations. This achieves the same decoupling with less boilerplate. Abstract Factory is still valuable when you need explicit, programmatic family selection or when you are not using a DI framework.
- **Service Locator:** A runtime registry that returns implementations by key. Less type-safe than Abstract Factory but more flexible when families are not known at compile time.

### Summary

The Abstract Factory pattern is one of the most powerful creational patterns when the problem genuinely involves **families of related objects**. Its strength — enforcing family consistency — is also its constraint: the product axis is hard to extend. Use it when you have a stable set of product types, multiple interchangeable families, and client code that must remain decoupled from all concrete implementations.
