# Prototype Pattern

**Classification:** Creational (GoF)

---

## 1. Core Idea

The Prototype pattern creates new objects by **copying an existing instance** (the "prototype") rather than instantiating from scratch via constructors.

### The Problem

Sometimes object creation is expensive. The cost might come from:

- **Heavy initialisation** -- reading config from disk, making network calls, computing random seeds, running complex setup logic.
- **Complex construction graphs** -- an object depends on a deep tree of other objects, each with their own setup.
- **Runtime-determined types** -- the client does not know (or should not know) the concrete class at compile time. It only has a reference to a base type and needs "another one like this."

In all these cases, calling `new ConcreteClass(...)` is either impractical or couples the client to the concrete type.

### The Key Insight

If you already have a fully-configured instance, you can treat it as a **template** and clone it. The clone is a new, independent object with the same state as the original. The client never needs to know which concrete class it is dealing with -- it just calls `clone()` on the prototype reference.

This decouples object creation from both the concrete class and the initialisation cost.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Prototype** | Declares the `clone()` method. Usually an interface or abstract class. |
| **ConcretePrototype** | Implements `clone()` by copying its own state into a new instance. |
| **Client** | Creates new objects by asking a prototype to clone itself, without knowing the concrete type. |

### How they relate

```
         +-----------------+
         |   <<interface>> |
         |    Prototype    |
         |-----------------|
         | + clone(): Self |
         +--------+--------+
                  |
       +----------+----------+
       |                      |
+------+--------+    +--------+------+
| ConcreteProto |    | ConcreteProto |
|      A        |    |      B        |
|---------------|    |---------------|
| + clone(): A  |    | + clone(): B  |
+---------------+    +---------------+

         Client
           |
           | holds a Prototype reference
           | calls prototype.clone()
           | receives a new instance
```

The Client holds one or more prototype references (often stored in a **registry** or **cache** keyed by name/type). When it needs a new object, it looks up the right prototype and clones it. At no point does the Client call a constructor directly for the product objects.

---

## 3. Use Cases

**Document / report templates.** A word processor stores template documents (invoice, memo, report). When the user picks "New from template," the system clones the template rather than rebuilding its paragraph tree, styles, headers, and footers from scratch.

**Game object spawning.** A game engine pre-loads prototypical enemies, projectiles, or terrain tiles during a loading screen. At runtime, spawning a new enemy is a clone operation -- far cheaper than re-parsing the sprite sheet, animation data, and AI config every time.

**Cached configuration objects.** An application fetches configuration from a remote service on startup and stores it as a prototype. Each request thread clones the config rather than making another network call. If the config changes, you replace the prototype; existing clones are unaffected.

**Dynamic plugin systems.** When the set of product types is not known at compile time (e.g. loaded from plugins or a database), you cannot write factory methods for every type. Instead, each plugin registers a prototypical instance; the framework clones it when a new instance is needed.

**Undo/snapshot systems.** Saving the state of a complex object for undo or checkpointing is essentially cloning. The Prototype pattern gives this a clean interface.

---

## 4. Example in Java

The domain here is a **game object spawning system**. We have different types of enemies that are expensive to initialise (imagine loading sprite data, computing pathfinding graphs, etc.). A registry pre-loads one prototype of each enemy type. At spawn time, we clone from the registry.

### The Prototype interface

```java
public interface EnemyPrototype {

    /**
     * Returns a deep copy of this enemy, ready to be placed in the game world.
     */
    EnemyPrototype clone();

    void setPosition(int x, int y);

    void describe();
}
```

### Concrete prototypes

```java
import java.util.ArrayList;
import java.util.List;

public class Goblin implements EnemyPrototype {

    private final String name;
    private final int baseHealth;
    private final List<String> abilities;   // mutable field -- must deep copy
    private int x;
    private int y;

    /** Full constructor -- simulates expensive initialisation. */
    public Goblin(String name, int baseHealth, List<String> abilities) {
        this.name = name;
        this.baseHealth = baseHealth;
        this.abilities = new ArrayList<>(abilities);
        // Imagine: loading sprite sheet, building nav mesh, etc.
        simulateExpensiveSetup();
    }

    /** Private copy constructor used by clone(). */
    private Goblin(Goblin other) {
        this.name = other.name;
        this.baseHealth = other.baseHealth;
        this.abilities = new ArrayList<>(other.abilities);  // deep copy of list
        this.x = 0;
        this.y = 0;
        // No expensive setup -- that is the whole point.
    }

    @Override
    public EnemyPrototype clone() {
        return new Goblin(this);
    }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void describe() {
        System.out.printf("Goblin [%s] hp=%d pos=(%d,%d) abilities=%s%n",
                name, baseHealth, x, y, abilities);
    }

    private void simulateExpensiveSetup() {
        try {
            Thread.sleep(500); // pretend this takes a while
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

```java
import java.util.ArrayList;
import java.util.List;

public class Dragon implements EnemyPrototype {

    private final String name;
    private final int baseHealth;
    private final String element;
    private final List<String> abilities;
    private int x;
    private int y;

    public Dragon(String name, int baseHealth, String element, List<String> abilities) {
        this.name = name;
        this.baseHealth = baseHealth;
        this.element = element;
        this.abilities = new ArrayList<>(abilities);
        simulateExpensiveSetup();
    }

    private Dragon(Dragon other) {
        this.name = other.name;
        this.baseHealth = other.baseHealth;
        this.element = other.element;
        this.abilities = new ArrayList<>(other.abilities);
        this.x = 0;
        this.y = 0;
    }

    @Override
    public EnemyPrototype clone() {
        return new Dragon(this);
    }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void describe() {
        System.out.printf("Dragon [%s] hp=%d element=%s pos=(%d,%d) abilities=%s%n",
                name, baseHealth, element, x, y, abilities);
    }

    private void simulateExpensiveSetup() {
        try {
            Thread.sleep(1000); // dragons are really expensive to load
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### Prototype registry

This is the cache that stores pre-built prototypes keyed by a name. The Client never calls constructors directly -- it goes through the registry.

```java
import java.util.HashMap;
import java.util.Map;

public class EnemyRegistry {

    private final Map<String, EnemyPrototype> prototypes = new HashMap<>();

    public void register(String key, EnemyPrototype prototype) {
        prototypes.put(key, prototype);
    }

    /**
     * Returns a clone of the registered prototype.
     * The caller gets a fresh, independent instance.
     */
    public EnemyPrototype spawn(String key) {
        EnemyPrototype prototype = prototypes.get(key);
        if (prototype == null) {
            throw new IllegalArgumentException("No prototype registered for: " + key);
        }
        return prototype.clone();
    }
}
```

### Client code

```java
import java.util.List;

public class GameClient {

    public static void main(String[] args) {
        EnemyRegistry registry = new EnemyRegistry();

        // --- Loading phase (expensive, happens once) ---
        System.out.println("Loading prototypes...");
        long start = System.currentTimeMillis();

        registry.register("goblin-scout", new Goblin(
                "Scout", 30, List.of("Sneak", "Dagger Throw")
        ));
        registry.register("goblin-warrior", new Goblin(
                "Warrior", 80, List.of("Shield Bash", "Cleave")
        ));
        registry.register("fire-dragon", new Dragon(
                "Ignis", 500, "Fire", List.of("Flame Breath", "Tail Sweep", "Fly")
        ));

        long loadTime = System.currentTimeMillis() - start;
        System.out.printf("Prototypes loaded in %d ms.%n%n", loadTime);

        // --- Spawning phase (cheap, happens many times) ---
        System.out.println("Spawning enemies...");
        start = System.currentTimeMillis();

        EnemyPrototype scout1 = registry.spawn("goblin-scout");
        scout1.setPosition(10, 20);

        EnemyPrototype scout2 = registry.spawn("goblin-scout");
        scout2.setPosition(50, 60);

        EnemyPrototype dragon = registry.spawn("fire-dragon");
        dragon.setPosition(200, 300);

        long spawnTime = System.currentTimeMillis() - start;
        System.out.printf("Enemies spawned in %d ms.%n%n", spawnTime);

        // --- Verify independence ---
        scout1.describe();
        scout2.describe();
        dragon.describe();

        System.out.println("\nscout1 == scout2? " + (scout1 == scout2)); // false
    }
}
```

### Expected output

```
Loading prototypes...
Prototypes loaded in ~2000 ms.

Spawning enemies...
Enemies spawned in ~0 ms.

Goblin [Scout] hp=30 pos=(10,20) abilities=[Sneak, Dagger Throw]
Goblin [Scout] hp=30 pos=(50,60) abilities=[Sneak, Dagger Throw]
Dragon [Ignis] hp=500 element=Fire pos=(200,300) abilities=[Flame Breath, Tail Sweep, Fly]

scout1 == scout2? false
```

The loading phase pays the initialisation cost once. Every subsequent spawn is just a copy -- orders of magnitude faster.

---

## 5. Tradeoffs and Limitations

### When to use the Prototype pattern

- Object creation is expensive and you can amortise the cost by cloning a pre-built instance.
- The client should not be coupled to concrete classes (similar motivation to Abstract Factory, but Prototype does not require a parallel factory hierarchy).
- The set of product types is determined at runtime (plugins, configuration, user-defined types).
- You need to produce objects whose state is "almost the same" as an existing object, with small tweaks after cloning.

### When NOT to use it

- **Simple construction.** If building an object is cheap and straightforward, cloning adds indirection for no benefit. Just use a constructor or a factory method.
- **Deeply nested mutable graphs.** If your object contains a web of mutable references (circular references, shared sub-objects), writing a correct deep copy is painful and error-prone. Consider the Builder pattern or immutable value objects instead.
- **When identity matters.** Cloning gives you a new object with duplicated state. If the domain requires unique IDs, database-assigned keys, or registration with a central system, the clone is not "ready to use" without post-processing -- which can negate the simplicity benefit.

### Shallow copy vs deep copy

This is the single most important implementation detail.

- A **shallow copy** duplicates the top-level object but shares references to nested objects. Mutating a nested object in the clone also mutates it in the original. This is almost never what you want for mutable fields.
- A **deep copy** recursively copies every mutable object in the graph. This is correct but can be expensive and tricky to implement when the graph has cycles or shared nodes.

Rule of thumb: **immutable fields can be shared safely; mutable fields must be deep-copied.** In the example above, `String` and `int` fields are safe to share (they are immutable/primitives), but the `List<String>` is mutable, so we copy it with `new ArrayList<>(other.abilities)`.

### The `Cloneable` / `Object.clone()` pitfalls in Java

Java provides a built-in cloning mechanism via `java.lang.Cloneable` and `Object.clone()`. In practice, it is widely considered **broken by design** and should usually be avoided. Here is why:

1. **`Cloneable` is a marker interface with no methods.** It does not declare `clone()`. The method lives on `Object` with `protected` access. You must override it, widen the access to `public`, and cast the return type. This is awkward and non-obvious.

2. **`Object.clone()` performs a shallow copy.** It copies field values verbatim. For reference fields, this means you get shared references -- a recipe for subtle aliasing bugs unless you manually deep-copy every mutable field.

3. **Constructor bypass.** `Object.clone()` creates an instance without calling any constructor. This can violate class invariants that the constructor is supposed to establish.

4. **Fragile with inheritance.** If a subclass forgets to override `clone()`, it inherits the parent's implementation, which returns an instance of the wrong type or misses newly added fields.

5. **Checked exception noise.** `Object.clone()` throws `CloneNotSupportedException`, which you must catch even when you know the class implements `Cloneable`.

The recommended alternative (and the one used in the example above) is a **copy constructor** or a **static factory method** that explicitly copies every field. This gives you full control, works naturally with the type system, and avoids every pitfall listed above.

```java
// Preferred: copy constructor
private Goblin(Goblin other) {
    this.name = other.name;
    this.baseHealth = other.baseHealth;
    this.abilities = new ArrayList<>(other.abilities);
}

// Avoid: Cloneable
public class Goblin implements Cloneable {
    @Override
    public Goblin clone() {
        try {
            Goblin copy = (Goblin) super.clone();
            copy.abilities = new ArrayList<>(this.abilities); // manual fix-up
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // should never happen
        }
    }
}
```

Joshua Bloch summarises it well in *Effective Java*: "The Cloneable architecture is incompatible with normal use of final fields referring to mutable objects." If a field is `final`, you cannot reassign it in the fix-up step after `super.clone()`. Copy constructors do not have this problem.

### Relationship to other patterns

- **Abstract Factory** can use Prototype internally -- instead of creating products via `new`, the factory clones prototypes.
- **Prototype** and **Builder** both address complex object creation, but Builder constructs step-by-step while Prototype copies wholesale.
- A **Prototype Registry** is essentially a specialised **Flyweight** pool, except the returned objects are independent copies rather than shared instances.
