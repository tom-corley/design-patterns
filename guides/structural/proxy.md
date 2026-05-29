# Proxy Pattern

**Classification:** Structural (GoF)

---

## 1. Core Idea

The Proxy pattern provides a surrogate or placeholder for another object in order to control access to it.

### The Problem

You have an object that is expensive to create, lives in a remote address space, requires access control, or needs additional bookkeeping -- but client code should not need to know or care about any of that. You want to interpose logic *before* and *after* calls reach the real object, without changing the real object or the client.

### The Key Insight

By giving the proxy the same interface as the real object, clients interact with it identically. The proxy holds a reference to the real object and forwards calls to it, adding its own logic around those calls. This indirection is invisible to the caller.

The pattern enforces a clean separation: the real object does its job, and the proxy handles the cross-cutting concern (laziness, security, caching, network transport, etc.).

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Subject** | The interface (or abstract class) that both the real object and the proxy implement. This is what clients program against. |
| **RealSubject** | The concrete class that does the actual work. The proxy eventually delegates to this. |
| **Proxy** | Holds a reference to the RealSubject. Implements the same interface and adds control logic around delegation. |

```
        +-------------------+
        |    <<interface>>  |
        |      Subject      |
        +-------------------+
        | + request()       |
        +-------------------+
              ^        ^
              |        |
   +----------+        +----------+
   |                              |
+--+------------+    +------------+--+
|  RealSubject  |    |     Proxy     |
+---------------+    +---------------+
| + request()   |    | - real: Subject|
+---------------+    | + request()    |
                     +----------------+
                        |
                        |  delegates to
                        +-----> RealSubject.request()
```

### Proxy Types

**Virtual Proxy** -- Controls access to an expensive-to-create object. The real object is instantiated only when it is first needed (lazy initialization). Classic example: loading a high-resolution image only when it is actually displayed.

**Protection Proxy** -- Controls access based on permissions. The proxy checks whether the caller has the rights to perform the requested operation before forwarding the call. Classic example: role-based access control on a service.

**Remote Proxy** -- Represents an object that lives in a different address space (another JVM, another machine, another network). The proxy handles serialization, transport, and deserialization. Classic example: Java RMI stubs, gRPC client stubs.

**Caching Proxy** -- Wraps a real object and caches results of expensive operations, returning cached values for repeated calls with the same inputs. Classic example: a proxy in front of a database or HTTP service.

**Logging / Smart Reference Proxy** -- Adds bookkeeping such as reference counting, call logging, or timing around delegation to the real object.

These are not mutually exclusive. A single proxy can combine virtual and caching behavior, for instance.

---

## 3. Use Cases

### Lazy Loading (Virtual Proxy)
An application displays a document with embedded images. Loading all images up front is slow. A virtual proxy stands in for each image, loading the actual pixel data from disk or network only when `display()` is called.

### Access Control (Protection Proxy)
A shared service exposes sensitive operations. A protection proxy wraps the service and checks the caller's role or token before forwarding each call. Unauthorized callers get an exception rather than reaching the real service.

### Remote Services (Remote Proxy)
A client application communicates with a service running on another machine. The proxy implements the same interface locally, handling network transport, serialization, error handling, and retries. Client code calls the proxy exactly as if the service were local.

### Caching (Caching Proxy)
A service queries a database or external API. A caching proxy intercepts calls, checks whether the result is already cached (and still valid), and returns the cached result if so. This reduces load on the underlying service and improves response times.

### Logging and Metrics
A proxy wraps a service to log every method call, record execution time, or count invocations. This is non-invasive instrumentation -- the real service remains unchanged.

---

## 4. Example in Java

The following example models a `UserRepository` that fetches user data from a database. We build two proxy variants around it:

1. **CachingProxy** -- caches results of `findById` to avoid repeated database hits.
2. **ProtectionProxy** -- checks the caller's role before allowing access to the repository.

### Subject Interface

```java
public interface UserRepository {
    User findById(long id);
    List<User> findAll();
    void save(User user);
}
```

### Domain Object

```java
public class User {
    private final long id;
    private final String name;
    private final String email;

    public User(long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}
```

### RealSubject -- DatabaseUserRepository

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulates a real database-backed repository.
 * Each call includes a deliberate delay to represent I/O cost.
 */
public class DatabaseUserRepository implements UserRepository {

    private final Map<Long, User> store = new HashMap<>();

    public DatabaseUserRepository() {
        // Seed with sample data
        store.put(1L, new User(1, "Alice", "alice@example.com"));
        store.put(2L, new User(2, "Bob", "bob@example.com"));
        store.put(3L, new User(3, "Charlie", "charlie@example.com"));
    }

    @Override
    public User findById(long id) {
        simulateLatency("findById(" + id + ")");
        return store.get(id);
    }

    @Override
    public List<User> findAll() {
        simulateLatency("findAll()");
        return new ArrayList<>(store.values());
    }

    @Override
    public void save(User user) {
        simulateLatency("save(" + user + ")");
        store.put(user.getId(), user);
        System.out.println("  [DB] Saved: " + user);
    }

    private void simulateLatency(String operation) {
        System.out.println("  [DB] Executing " + operation + " ...");
        try {
            Thread.sleep(500); // simulate I/O
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### Proxy Variant 1 -- CachingUserRepository

```java
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caching proxy for UserRepository.
 *
 * Caches results of findById. The cache is invalidated for a given user
 * whenever save() is called for that user. findAll() is never cached
 * because the result set changes with every save.
 */
public class CachingUserRepository implements UserRepository {

    private final UserRepository delegate;
    private final Map<Long, User> cache = new HashMap<>();

    public CachingUserRepository(UserRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public User findById(long id) {
        if (cache.containsKey(id)) {
            System.out.println("  [CACHE] Hit for id=" + id);
            return cache.get(id);
        }
        System.out.println("  [CACHE] Miss for id=" + id);
        User user = delegate.findById(id);
        if (user != null) {
            cache.put(id, user);
        }
        return user;
    }

    @Override
    public List<User> findAll() {
        // Not cached -- delegate directly
        return delegate.findAll();
    }

    @Override
    public void save(User user) {
        // Invalidate cache entry, then delegate
        cache.remove(user.getId());
        delegate.save(user);
    }
}
```

### Proxy Variant 2 -- ProtectedUserRepository

```java
import java.util.List;

/**
 * Protection proxy for UserRepository.
 *
 * Enforces role-based access: only users with the ADMIN role may call save().
 * Read operations (findById, findAll) are allowed for any authenticated caller.
 */
public class ProtectedUserRepository implements UserRepository {

    private final UserRepository delegate;
    private final CallerContext callerContext;

    public ProtectedUserRepository(UserRepository delegate, CallerContext callerContext) {
        this.delegate = delegate;
        this.callerContext = callerContext;
    }

    @Override
    public User findById(long id) {
        requireAuthenticated();
        return delegate.findById(id);
    }

    @Override
    public List<User> findAll() {
        requireAuthenticated();
        return delegate.findAll();
    }

    @Override
    public void save(User user) {
        requireAuthenticated();
        requireRole(Role.ADMIN);
        delegate.save(user);
    }

    private void requireAuthenticated() {
        if (callerContext == null || callerContext.getRole() == null) {
            throw new SecurityException("Authentication required");
        }
    }

    private void requireRole(Role required) {
        if (callerContext.getRole() != required) {
            throw new SecurityException(
                "Access denied: required role " + required
                + ", but caller has " + callerContext.getRole()
            );
        }
    }
}
```

### Supporting Types for the Protection Proxy

```java
public enum Role {
    VIEWER, EDITOR, ADMIN
}
```

```java
public class CallerContext {
    private final String username;
    private final Role role;

    public CallerContext(String username, Role role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() { return username; }
    public Role getRole() { return role; }
}
```

### Client Code -- Main

```java
import java.util.List;

public class Main {
    public static void main(String[] args) {
        UserRepository db = new DatabaseUserRepository();

        // --- Caching Proxy Demo ---
        System.out.println("=== Caching Proxy ===\n");

        UserRepository cachedRepo = new CachingUserRepository(db);

        System.out.println("First lookup (cache miss, hits DB):");
        User alice = cachedRepo.findById(1);
        System.out.println("  Result: " + alice + "\n");

        System.out.println("Second lookup (cache hit, no DB call):");
        User aliceAgain = cachedRepo.findById(1);
        System.out.println("  Result: " + aliceAgain + "\n");

        System.out.println("Save invalidates cache:");
        cachedRepo.save(new User(1, "Alice Updated", "alice.updated@example.com"));
        System.out.println();

        System.out.println("Lookup after save (cache miss again):");
        User aliceUpdated = cachedRepo.findById(1);
        System.out.println("  Result: " + aliceUpdated + "\n");

        // --- Protection Proxy Demo ---
        System.out.println("=== Protection Proxy ===\n");

        CallerContext viewer = new CallerContext("tom", Role.VIEWER);
        CallerContext admin  = new CallerContext("admin-jane", Role.ADMIN);

        // Stack the proxies: protection on top of caching on top of real DB
        UserRepository securedRepo = new ProtectedUserRepository(
            new CachingUserRepository(db), viewer
        );

        System.out.println("Viewer reads (allowed):");
        User bob = securedRepo.findById(2);
        System.out.println("  Result: " + bob + "\n");

        System.out.println("Viewer tries to save (denied):");
        try {
            securedRepo.save(new User(4, "Dave", "dave@example.com"));
        } catch (SecurityException e) {
            System.out.println("  SecurityException: " + e.getMessage() + "\n");
        }

        // Now with admin context
        UserRepository adminRepo = new ProtectedUserRepository(
            new CachingUserRepository(db), admin
        );

        System.out.println("Admin saves (allowed):");
        adminRepo.save(new User(4, "Dave", "dave@example.com"));
        System.out.println();

        System.out.println("Admin reads back the saved user:");
        User dave = adminRepo.findById(4);
        System.out.println("  Result: " + dave);
    }
}
```

### Expected Output (approximate)

```
=== Caching Proxy ===

First lookup (cache miss, hits DB):
  [CACHE] Miss for id=1
  [DB] Executing findById(1) ...
  Result: User{id=1, name='Alice', email='alice@example.com'}

Second lookup (cache hit, no DB call):
  [CACHE] Hit for id=1
  Result: User{id=1, name='Alice', email='alice@example.com'}

Save invalidates cache:
  [DB] Executing save(User{id=1, name='Alice Updated', email='alice.updated@example.com'}) ...
  [DB] Saved: User{id=1, name='Alice Updated', email='alice.updated@example.com'}

Lookup after save (cache miss again):
  [CACHE] Miss for id=1
  [DB] Executing findById(1) ...
  Result: User{id=1, name='Alice Updated', email='alice.updated@example.com'}

=== Protection Proxy ===

Viewer reads (allowed):
  [CACHE] Miss for id=2
  [DB] Executing findById(2) ...
  Result: User{id=2, name='Bob', email='bob@example.com'}

Viewer tries to save (denied):
  SecurityException: Access denied: required role ADMIN, but caller has VIEWER

Admin saves (allowed):
  [DB] Executing save(User{id=4, name='Dave', email='dave@example.com'}) ...
  [DB] Saved: User{id=4, name='Dave', email='dave@example.com'}

Admin reads back the saved user:
  [CACHE] Miss for id=4
  [DB] Executing findById(4) ...
  Result: User{id=4, name='Dave', email='dave@example.com'}
```

### Key Observations from the Example

- **Same interface everywhere.** `CachingUserRepository`, `ProtectedUserRepository`, and `DatabaseUserRepository` all implement `UserRepository`. Client code does not know which implementation it holds.
- **Proxies compose.** The `Main` class stacks protection on top of caching on top of the real DB. Each proxy adds one concern. This is possible precisely because every layer shares the same interface.
- **Cache invalidation is explicit.** The caching proxy invalidates on `save()`. A more sophisticated implementation might use TTLs or event-driven invalidation.
- **The protection proxy is stateless with respect to business data.** It only inspects the caller context and delegates or rejects. The real authorization logic lives in the proxy; the real business logic lives in the RealSubject.

---

## 5. Tradeoffs and Limitations

### When to Use the Proxy Pattern

- You need lazy initialization of an expensive object and want it transparent to clients.
- You need to enforce access control without modifying the real service.
- You need caching, logging, or metrics around an existing service without changing its code.
- You are wrapping a remote service behind a local interface (RPC stubs, REST client wrappers).
- You want to compose multiple cross-cutting concerns (caching + auth + logging) by stacking proxies.

### When Not to Use It

- **The real object is cheap to create and has no access concerns.** Adding a proxy is pure overhead for no benefit.
- **You need to alter the object's interface or add new operations.** Proxy preserves the interface; if you need a different one, look at Adapter or Facade.
- **You have many methods and the proxy logic is identical for all of them.** Hand-writing a proxy with 20 forwarding methods is tedious. In Java, consider `java.lang.reflect.Proxy` (dynamic proxy) or a bytecode generation library (CGLIB, Byte Buddy) instead of writing a static proxy class.
- **The cross-cutting concern applies globally across many unrelated services.** Aspect-oriented programming (AOP) or interceptors (e.g., Spring AOP, Jakarta Interceptors) may be a better fit than manually wrapping each service.

### Performance Overhead

Every proxy adds one layer of indirection per method call. For a single proxy this is negligible. When proxies are stacked deeply (caching + auth + logging + retry + circuit-breaker), the cost of repeated delegation can become measurable in tight loops or latency-sensitive paths. Profile before optimizing.

A caching proxy can *improve* performance dramatically by avoiding expensive I/O, but a poorly tuned cache (wrong eviction policy, no TTL, unbounded size) can cause memory issues or serve stale data.

### Proxy vs Decorator

Proxy and Decorator are structurally almost identical -- both wrap an object behind the same interface and delegate to it. The difference is in intent:

| | Proxy | Decorator |
|---|---|---|
| **Intent** | Control *access* to the object (creation, security, location, caching) | Add *new behavior or responsibilities* to the object |
| **Who creates the real object?** | Often the proxy itself (especially virtual proxies that lazily instantiate the real object) | The client typically creates the real object and wraps it |
| **Transparency** | The client usually does not know a proxy is involved | The client explicitly composes decorators |
| **Lifecycle** | The proxy may manage the real object's lifecycle (create, pool, destroy) | The decorator does not manage lifecycle |

In practice the line can blur. A caching proxy looks a lot like a decorator that adds caching behavior. The pattern name you choose should reflect your primary intent: if you are *controlling access*, call it a Proxy; if you are *enriching behavior*, call it a Decorator.

### Relationship to Other Patterns

- **Adapter** changes the interface; Proxy preserves it.
- **Facade** simplifies a complex subsystem behind a new, simpler interface; Proxy wraps a single object behind the *same* interface.
- **Flyweight** shares intrinsic state across many objects to save memory; a virtual proxy defers creation of a single expensive object. They solve different resource problems but can coexist (a flyweight factory might return proxies for not-yet-loaded objects).

The Proxy pattern is one of the most practical structural patterns. Its power comes from a simple idea -- same interface, controlled delegation -- applied to a wide range of cross-cutting concerns. The fact that proxies compose (stack) cleanly makes them a natural building block in layered architectures. The main risk is overuse: if you find yourself writing trivial forwarding methods across a large interface, consider dynamic proxies or AOP instead.
