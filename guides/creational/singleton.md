# Singleton Pattern

**Classification:** Creational (GoF)

---

## 1. Core Idea

The Singleton pattern ensures a class has **exactly one instance** and provides a **global point of access** to it.

### The Problem

Some objects should exist only once across the entire lifetime of an application. A database connection pool, an application-wide logger, or a runtime configuration registry are all examples where having multiple instances would be wasteful, error-prone, or semantically wrong. Without a controlled mechanism, any piece of code could freely instantiate these classes, leading to duplicated resources, inconsistent state, or subtle concurrency bugs.

### The Key Insight

If the class itself controls its own instantiation -- by hiding its constructor and exposing a static access method -- then it can guarantee that no more than one instance ever exists. The class becomes its own factory, creating the instance on first request (or at class-load time) and returning that same instance on every subsequent call.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Singleton** | Declares a private static field holding the sole instance. Declares a private constructor to prevent external instantiation. Exposes a public static `getInstance()` method that returns the unique instance, creating it if necessary. |
| **Clients** | Access the singleton exclusively through `getInstance()`. They never call `new` on the Singleton class. |

### How the single instance is managed

1. The constructor is `private`, so no external code can call `new Singleton()`.
2. A `private static` field stores the one-and-only instance.
3. A `public static` method (`getInstance()`) returns that field, optionally creating the instance on first access.
4. In multithreaded environments, the creation step must be synchronized or otherwise made safe to avoid creating duplicate instances.

### Class diagram (textual)

```
+-----------------------------+
|         Singleton           |
+-----------------------------+
| - instance: Singleton       |
+-----------------------------+
| - Singleton()               |
| + getInstance(): Singleton  |
| + businessMethod(): void    |
+-----------------------------+
```

---

## 3. Use Cases

### Logging

A logging framework typically funnels all log output through a single coordinating object that manages file handles, formatting, and log levels. Multiple logger instances competing for the same file would corrupt output or require expensive external synchronization.

### Application configuration

A configuration registry loaded once from a file, environment variables, or a remote service should be read-only and shared everywhere. Reloading and re-parsing configuration in every consumer is wasteful and risks inconsistency if the source changes mid-run.

### Connection pools

A database connection pool manages a fixed set of expensive connections. If every module created its own pool, the application would exhaust the database's connection limit and defeat the purpose of pooling entirely.

### Thread pools / Executor services

Similar to connection pools, a shared thread pool prevents runaway thread creation. A singleton executor service gives the application a single place to tune concurrency limits.

### Caches

An in-memory cache (e.g., a map of recently computed results) only delivers value if every caller hits the same cache. Separate instances would mean separate caches with no shared hits.

### Hardware interface access

When software controls a physical resource -- a printer spooler, a GPU context, a serial port -- a singleton prevents conflicting commands from multiple owners.

---

## 4. Example in Java

### Approach 1: Eager Initialization

```java
public final class EagerSingleton {

    // Instance created at class-loading time.
    private static final EagerSingleton INSTANCE = new EagerSingleton();

    // Private constructor prevents external instantiation.
    private EagerSingleton() {
        // initialization logic
    }

    public static EagerSingleton getInstance() {
        return INSTANCE;
    }

    public void doWork() {
        System.out.println("EagerSingleton doing work");
    }
}
```

**Thread safety:** Fully thread-safe with zero synchronization cost. The JVM guarantees that static final fields are initialized exactly once during class loading, which is inherently thread-safe. The JLS (Java Language Specification, Section 12.4.2) ensures that class initialization is performed under a lock.

**Tradeoff:** The instance is created whether or not it is ever used. If construction is expensive and the singleton may never be needed, this wastes resources. In practice, this is rarely a problem -- classes are not loaded until they are first referenced.

---

### Approach 2: Lazy Initialization with Double-Checked Locking

```java
public final class LazySingleton {

    // volatile ensures visibility of the fully constructed object across threads.
    private static volatile LazySingleton instance;

    private LazySingleton() {
        // initialization logic
    }

    public static LazySingleton getInstance() {
        // First check (no locking) -- fast path for the common case.
        if (instance == null) {
            synchronized (LazySingleton.class) {
                // Second check (under lock) -- prevents duplicate creation.
                if (instance == null) {
                    instance = new LazySingleton();
                }
            }
        }
        return instance;
    }

    public void doWork() {
        System.out.println("LazySingleton doing work");
    }
}
```

**Thread safety:** Thread-safe, provided the `volatile` keyword is present. Without `volatile`, the JVM is permitted to reorder the writes inside the constructor and the assignment to `instance`. Another thread could see a non-null `instance` reference that points to a partially constructed object. The `volatile` modifier establishes a happens-before relationship that prevents this reordering.

**Why double-checked?** Synchronizing the entire `getInstance()` method would work but forces every call -- even after the instance exists -- to acquire a lock. The outer null check lets threads skip synchronization entirely once the instance is initialized. The inner null check ensures only one thread actually creates the instance.

**Note:** Double-checked locking was broken in Java before the Java 5 memory model revision (JSR-133). It is safe from Java 5 onward with `volatile`.

---

### Approach 3: Initialization-on-Demand Holder (Bill Pugh Singleton)

```java
public final class HolderSingleton {

    private HolderSingleton() {
        // initialization logic
    }

    // Inner static class is not loaded until getInstance() is called.
    private static final class Holder {
        private static final HolderSingleton INSTANCE = new HolderSingleton();
    }

    public static HolderSingleton getInstance() {
        return Holder.INSTANCE;
    }

    public void doWork() {
        System.out.println("HolderSingleton doing work");
    }
}
```

**Thread safety:** Fully thread-safe with no synchronization overhead. This exploits the same class-loading guarantee as eager initialization, but defers it: the JVM will not load the inner `Holder` class until `getInstance()` is called for the first time. This gives you lazy initialization and thread safety without `volatile` or `synchronized`.

**This is widely regarded as the best pre-enum idiom** for lazy singletons in Java.

---

### Approach 4: Enum Singleton (Recommended)

```java
public enum EnumSingleton {

    INSTANCE;

    // State and behavior, just like a normal class.
    private int requestCount = 0;

    public void doWork() {
        requestCount++;
        System.out.println("EnumSingleton doing work. Request #" + requestCount);
    }

    public int getRequestCount() {
        return requestCount;
    }
}
```

**Usage:**

```java
public class Client {
    public static void main(String[] args) {
        EnumSingleton singleton = EnumSingleton.INSTANCE;
        singleton.doWork();
        singleton.doWork();
        System.out.println("Total requests: " + singleton.getRequestCount());
    }
}
```

**Thread safety:** Fully thread-safe. Enum constants are guaranteed by the JVM to be instantiated exactly once, even in the face of complex class-loading scenarios, reflection, and serialization.

**Why this is the recommended approach (per Joshua Bloch, Effective Java):**

1. **Serialization safety for free.** All other approaches require implementing `readResolve()` to prevent deserialization from creating a second instance. Enums handle this automatically.
2. **Reflection safety for free.** `Constructor.newInstance()` on an enum type throws an `IllegalArgumentException`. With the other approaches, a determined caller can use reflection to set the constructor accessible and break the singleton guarantee.
3. **Concise.** No boilerplate: no private constructor, no static field, no `getInstance()`.

**Limitation:** An enum cannot extend another class (it implicitly extends `java.lang.Enum`). If your singleton must inherit from a base class, you need one of the other approaches.

---

### Comparison table

| Approach | Lazy? | Thread-safe? | Reflection-safe? | Serialization-safe? | Complexity |
|---|---|---|---|---|---|
| Eager | No | Yes | No | No | Low |
| Double-checked locking | Yes | Yes (Java 5+) | No | No | Medium |
| Holder (Bill Pugh) | Yes | Yes | No | No | Low |
| Enum | No | Yes | Yes | Yes | Lowest |

---

## 5. Tradeoffs and Limitations

### Testability problems

Singletons are notoriously difficult to unit test:

- **Hidden dependencies.** When a class calls `Logger.getInstance()` deep inside its methods, the dependency on `Logger` is invisible from the constructor or method signature. Tests cannot easily substitute a mock or stub.
- **Shared mutable state across tests.** Because the instance persists for the lifetime of the JVM, state accumulated in one test leaks into the next. Test ordering becomes significant, and parallel test execution becomes hazardous.
- **No interface by default.** The `getInstance()` call couples consumers to the concrete class. Without an interface, you cannot swap in a test double without modifying the singleton itself.

A partial workaround is to have the singleton implement an interface and provide a package-private `resetForTesting()` method, but this is a code smell that signals the design is fighting against itself.

### Hidden dependencies and tight coupling

Singletons accessed via static methods create implicit, compile-time coupling between every consumer and the concrete singleton class. This makes it hard to:

- Understand what a class depends on by reading its constructor.
- Refactor the singleton into a different implementation.
- Reuse a class in a context where the singleton does not apply (e.g., a different application or module).

### Global mutable state

A singleton with mutable fields is effectively a global variable with object-oriented syntax. Global mutable state is a well-understood source of bugs, especially in concurrent programs. The same criticisms leveled at global variables -- unpredictable modification, action at a distance, reasoning difficulty -- apply directly.

### The "anti-pattern" reputation

Many experienced developers consider Singleton an anti-pattern rather than a pattern. The core arguments are:

1. **It violates the Single Responsibility Principle.** The class manages its own lifecycle in addition to its actual business responsibility.
2. **It makes code harder to reason about.** Any method anywhere in the codebase can silently depend on and mutate singleton state.
3. **It hinders concurrency.** Even a thread-safe singleton can become a contention bottleneck if many threads funnel through it.
4. **It resists change.** Promoting a singleton to "allow two instances" requires reworking every call site.

### Alternatives: Dependency Injection

The modern alternative to Singleton is **dependency injection (DI)**. Instead of a class reaching out to a global access point, it declares its dependency in its constructor:

```java
public class OrderService {

    private final Logger logger;

    // The dependency is explicit, injectable, and mockable.
    public OrderService(Logger logger) {
        this.logger = logger;
    }

    public void placeOrder(Order order) {
        logger.info("Placing order: " + order.getId());
        // ...
    }
}
```

A DI framework (Spring, Guice, Dagger) or manual wiring at the composition root can then ensure that only one `Logger` instance is created and shared -- achieving the "single instance" goal without any of the downsides of the Singleton pattern. The key difference: **the container controls the lifecycle, not the class itself.**

This gives you:

- **Explicit dependencies** visible in constructors.
- **Easy testing** by passing mocks or stubs.
- **Flexible scoping** -- singleton scope, request scope, or prototype scope, configurable externally.
- **No static coupling** between consumer and provider.

### When Singleton is still appropriate

Despite its drawbacks, the pattern remains a reasonable choice when:

- You are writing framework or infrastructure code where DI is not available (e.g., a logging library that must bootstrap before any DI container exists).
- The singleton is truly stateless or immutable after initialization (e.g., a configuration object loaded once and never modified).
- You are working in a constrained environment where simplicity outweighs testability concerns.

Even in these cases, prefer the enum approach for its safety guarantees and simplicity.
