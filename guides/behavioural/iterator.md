# Iterator Pattern

**Classification:** Behavioural (GoF)

**Also known as:** Cursor

---

## 1. Core Idea

The Iterator pattern provides a way to access the elements of an aggregate object sequentially without exposing its underlying representation. The traversal logic is extracted into a separate object -- the iterator -- so that the collection itself does not need to know how clients walk through its elements, and clients do not need to know whether they are traversing an array, a linked list, a tree, or a database result set.

### The Problem

Imagine you have built a custom collection -- say, a sensor network that stores readings in a circular buffer. Client code needs to iterate over those readings: display them, compute averages, filter outliers. If the client accesses the buffer directly, it becomes coupled to the internal data structure. Change the buffer to a linked list, or switch to a paginated remote store, and every client breaks.

Worse, different clients may need different traversal orders (chronological, reverse, filtered by threshold). Embedding all of those traversal strategies inside the collection bloats its interface and violates the Single Responsibility Principle.

### The Key Insight

Separate the *traversal responsibility* from the *storage responsibility*. Define a common iterator interface (`hasNext()`, `next()`) that all traversals implement, and let the collection hand out iterators without revealing how its data is organized. The client programs to the iterator interface and is indifferent to the collection's internals. Multiple independent iterators can traverse the same collection concurrently, each maintaining its own position.

This is the same principle behind Java's `java.util.Iterator` and the enhanced for-loop: the language itself bakes in the pattern because it is so universally useful.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Iterator** | Defines the interface for accessing and traversing elements: `hasNext()`, `next()`, and optionally `remove()`. |
| **ConcreteIterator** | Implements the `Iterator` interface. Tracks the current position in the traversal and knows how to advance to the next element. |
| **Aggregate** (also called Iterable/Collection) | Defines an interface for creating an `Iterator` object: `createIterator()` (or `iterator()` in Java). |
| **ConcreteAggregate** | Implements the `Aggregate` interface. Returns an instance of the appropriate `ConcreteIterator`. Holds the actual data structure. |

### Relationships

```
    +---------------------+             +---------------------+
    |     Aggregate        |             |      Iterator        |
    |---------------------|             |---------------------|
    | + createIterator()  |--- creates -->| + hasNext(): boolean|
    +---------------------+             | + next(): E         |
              ^                         +---------------------+
              |                                   ^
              |                                   |
  +--------------------------+      +--------------------------+
  |   ConcreteAggregate      |      |   ConcreteIterator       |
  |--------------------------|      |--------------------------|
  | - elements: [data]       |      | - aggregate: ref         |
  | + createIterator()       |----->| - currentIndex: int      |
  +--------------------------+      | + hasNext(): boolean     |
                                    | + next(): E             |
                                    +--------------------------+
```

The `ConcreteAggregate` creates a `ConcreteIterator` and passes a reference to itself (or to its internal data) so the iterator can walk the elements. The client holds only the `Iterator` interface and calls `hasNext()`/`next()` in a loop. It never touches the aggregate's internal structure.

Key points:

- The iterator holds traversal state (current index, pointer, cursor position), not the collection.
- Multiple iterators on the same collection are independent; each has its own position.
- The collection's only iterator-related responsibility is creating one.

---

## 3. Use Cases

**Collection traversal with hidden representation.** The classic case. A custom data structure (circular buffer, skip list, trie, priority queue) needs to offer sequential access without clients knowing the implementation. The iterator adapts the internal structure to a linear stream of elements.

**Database cursors and paginated APIs.** A query returns 10,000 rows but you process them one at a time. The iterator encapsulates cursor management, page fetching, and connection lifecycle. The client sees only `hasNext()` / `next()` and never deals with offsets, tokens, or connection pooling. JDBC's `ResultSet` is essentially this pattern.

**Tree and graph traversal.** A DOM tree, an AST, or a file system can be traversed depth-first, breadth-first, in-order, or post-order. Each strategy is a separate `ConcreteIterator` over the same tree aggregate. The client picks the traversal it needs and processes nodes identically regardless of order.

**Composite structures.** When the Composite pattern is used to build recursive tree structures (menus, organizational hierarchies, UI widget trees), an iterator provides a flat, sequential view over a deeply nested structure. This is much easier for clients than writing recursive visits themselves.

**Streaming and lazy evaluation.** An iterator that computes or fetches elements on demand avoids loading an entire dataset into memory. Infinite sequences (Fibonacci numbers, log streams, event feeds) become practical because the iterator produces values only when `next()` is called.

**Uniform multi-collection iteration.** Several heterogeneous collections (an array, a linked list, a set) need to be traversed in the same loop. If all implement the same Aggregate/Iterable interface, client code is written once and works against any of them.

---

## 4. Example in Java

This example models a **paginated API result iterator**. A `NotificationInbox` fetches notifications from a remote service in pages (batches) but exposes them to the client as a simple, linear `Iterator<Notification>` and `Iterable<Notification>`. The client never knows that pagination is happening behind the scenes.

### The element (value object)

```java
/**
 * Represents a single notification in the inbox.
 */
public record Notification(String id, String sender, String message, long timestamp) {

    @Override
    public String toString() {
        return "[" + id + "] " + sender + ": " + message;
    }
}
```

### The Aggregate -- implementing java.lang.Iterable

```java
import java.util.Iterator;

/**
 * Aggregate. Represents a paginated notification inbox. Implements
 * {@link Iterable} so it can be used in enhanced for-loops.
 *
 * Internally, notifications are fetched in pages of a fixed size.
 * The iterator handles page boundaries transparently.
 */
public class NotificationInbox implements Iterable<Notification> {

    private final NotificationService service;
    private final String userId;
    private final int pageSize;

    public NotificationInbox(NotificationService service, String userId, int pageSize) {
        this.service = service;
        this.userId = userId;
        this.pageSize = pageSize;
    }

    /**
     * Factory method for the iterator. Each call returns a fresh iterator
     * starting from the first page.
     */
    @Override
    public Iterator<Notification> iterator() {
        return new PaginatedNotificationIterator(service, userId, pageSize);
    }
}
```

### The ConcreteIterator -- handling pagination internally

```java
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * ConcreteIterator. Lazily fetches pages of notifications from the
 * service and yields them one at a time.
 *
 * The client calls hasNext()/next() and is unaware of page boundaries,
 * offsets, or batch sizes.
 */
public class PaginatedNotificationIterator implements Iterator<Notification> {

    private final NotificationService service;
    private final String userId;
    private final int pageSize;

    private List<Notification> currentPage;
    private int indexInPage;
    private int pageOffset;
    private boolean exhausted;

    public PaginatedNotificationIterator(NotificationService service,
                                         String userId,
                                         int pageSize) {
        this.service = service;
        this.userId = userId;
        this.pageSize = pageSize;
        this.currentPage = List.of();
        this.indexInPage = 0;
        this.pageOffset = 0;
        this.exhausted = false;
    }

    @Override
    public boolean hasNext() {
        if (indexInPage < currentPage.size()) {
            return true;
        }
        if (exhausted) {
            return false;
        }
        // Fetch the next page
        fetchNextPage();
        return !currentPage.isEmpty();
    }

    @Override
    public Notification next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more notifications");
        }
        Notification notification = currentPage.get(indexInPage);
        indexInPage++;
        return notification;
    }

    private void fetchNextPage() {
        currentPage = service.fetchPage(userId, pageOffset, pageSize);
        indexInPage = 0;

        if (currentPage.size() < pageSize) {
            // Last page -- fewer results than requested means no more data.
            exhausted = true;
        }
        pageOffset += currentPage.size();
    }
}
```

### The backing service (simulated)

```java
import java.util.ArrayList;
import java.util.List;

/**
 * Simulates a remote notification service that returns results in pages.
 * In production this would be an HTTP client or database query.
 */
public class NotificationService {

    private final List<Notification> allNotifications;

    public NotificationService() {
        // Seed with some sample data
        allNotifications = new ArrayList<>();
        allNotifications.add(new Notification("n1", "Alice",   "Hey, are you coming to the meeting?",   1));
        allNotifications.add(new Notification("n2", "Bob",     "PR #42 has been approved.",              2));
        allNotifications.add(new Notification("n3", "Charlie", "Deploy to staging is complete.",         3));
        allNotifications.add(new Notification("n4", "Alice",   "Updated the design doc.",               4));
        allNotifications.add(new Notification("n5", "Diana",   "New ticket assigned to you.",            5));
        allNotifications.add(new Notification("n6", "Bob",     "Build failed on main.",                  6));
        allNotifications.add(new Notification("n7", "Eve",     "Invited you to #backend-chat.",          7));
    }

    /**
     * Returns a page of notifications starting at the given offset.
     *
     * @param userId ignored in this simulation, but would filter in production
     * @param offset zero-based offset into the full result set
     * @param limit  maximum number of results to return
     * @return a list of up to {@code limit} notifications
     */
    public List<Notification> fetchPage(String userId, int offset, int limit) {
        System.out.println("  [Service] Fetching page: offset=" + offset + ", limit=" + limit);

        if (offset >= allNotifications.size()) {
            return List.of();
        }
        int end = Math.min(offset + limit, allNotifications.size());
        return List.copyOf(allNotifications.subList(offset, end));
    }
}
```

### Client code

```java
public class Main {

    public static void main(String[] args) {
        NotificationService service = new NotificationService();

        // Page size of 3: the iterator will make multiple fetches behind the scenes
        NotificationInbox inbox = new NotificationInbox(service, "user-123", 3);

        // --- Using the enhanced for-loop (possible because NotificationInbox is Iterable) ---
        System.out.println("=== All notifications (for-each loop) ===");
        for (Notification n : inbox) {
            System.out.println("  " + n);
        }

        System.out.println();

        // --- Using the iterator explicitly ---
        System.out.println("=== First 4 notifications (explicit iterator) ===");
        var it = inbox.iterator();
        int count = 0;
        while (it.hasNext() && count < 4) {
            System.out.println("  " + it.next());
            count++;
        }
        // The iterator can be abandoned mid-traversal. No cleanup needed.

        System.out.println();

        // --- Two independent iterators on the same inbox ---
        System.out.println("=== Independent iterators ===");
        var it1 = inbox.iterator();
        var it2 = inbox.iterator();
        System.out.println("  it1: " + it1.next());
        System.out.println("  it1: " + it1.next());
        System.out.println("  it2: " + it2.next());  // starts from the beginning
        System.out.println("  it1: " + it1.next());
    }
}
```

### Expected output

```
=== All notifications (for-each loop) ===
  [Service] Fetching page: offset=0, limit=3
  [n1] Alice: Hey, are you coming to the meeting?
  [n2] Bob: PR #42 has been approved.
  [n3] Charlie: Deploy to staging is complete.
  [Service] Fetching page: offset=3, limit=3
  [n4] Alice: Updated the design doc.
  [n5] Diana: New ticket assigned to you.
  [n6] Bob: Build failed on main.
  [Service] Fetching page: offset=6, limit=3
  [n7] Eve: Invited you to #backend-chat.
  [Service] Fetching page: offset=7, limit=3

=== First 4 notifications (explicit iterator) ===
  [Service] Fetching page: offset=0, limit=3
  [n1] Alice: Hey, are you coming to the meeting?
  [n2] Bob: PR #42 has been approved.
  [n3] Charlie: Deploy to staging is complete.
  [Service] Fetching page: offset=3, limit=3
  [n4] Alice: Updated the design doc.

=== Independent iterators ===
  [Service] Fetching page: offset=0, limit=3
  [n1] Alice: Hey, are you coming to the meeting?
  [n2] Bob: PR #42 has been approved.
  [Service] Fetching page: offset=0, limit=3
  [n3] Charlie: Deploy to staging is complete.
  [n1] Alice: Hey, are you coming to the meeting?
```

Wait -- the last two lines need a closer look. `it2.next()` returns `n1` (it2 starts from the beginning), and `it1.next()` returns `n3` (it1 continues from where it left off). This demonstrates that the two iterators are independent, each maintaining their own page cache and position:

```
  it2: [n1] Alice: Hey, are you coming to the meeting?
  it1: [n3] Charlie: Deploy to staging is complete.
```

### Connecting to Java's built-in interfaces

Java's standard library already encodes this pattern in two interfaces:

- **`java.lang.Iterable<T>`** is the Aggregate role. Any class that implements it can be used in enhanced for-loops (`for (T item : collection)`). It declares a single method: `Iterator<T> iterator()`.
- **`java.util.Iterator<T>`** is the Iterator role. It declares `hasNext()`, `next()`, and the optional default method `remove()`.

By having `NotificationInbox` implement `Iterable<Notification>` and returning a `PaginatedNotificationIterator` (which implements `Iterator<Notification>`), the example plugs directly into the language. Every piece of Java code that works with `Iterable` -- for-each loops, `StreamSupport.stream()`, Guava utilities, Spring's collection-binding -- works with our inbox automatically.

This is the power of the pattern: a custom, lazily-paginated data source becomes interchangeable with an `ArrayList` or a `TreeSet` at the iteration level.

---

## 5. Tradeoffs and Limitations

### When to use it

- The collection's internal representation should be hidden from clients (encapsulation).
- You need multiple simultaneous traversals over the same collection, each with its own state.
- You want to support multiple traversal strategies (forward, reverse, filtered, depth-first, breadth-first) without bloating the collection's interface.
- Lazy or on-demand element production is needed (pagination, streaming, infinite sequences).
- You want to unify traversal across heterogeneous data structures behind a single interface.

### When not to use it

- **Simple arrays or standard collections.** If you are using `ArrayList`, `HashSet`, or any `java.util.Collection`, the iterator is already provided. Writing a custom one adds complexity with no benefit.
- **Random access is the primary use case.** Iterators are inherently sequential. If the client needs to jump to index 500 or binary-search the collection, an iterator is the wrong abstraction. Provide indexed access or `List`-style methods instead.
- **Single-pass, throw-away traversal.** If there is only one consumer, only one traversal order, and no need for encapsulation, a simple loop over the internal array is fine. The pattern's overhead is not justified.

### Concurrent modification

This is the biggest practical hazard. If the underlying collection is modified while an iterator is active, the iterator's position may become invalid -- it could skip elements, repeat elements, or throw an `ArrayIndexOutOfBoundsException`.

Java's standard collections address this with **fail-fast iterators**: if the collection detects structural modification after the iterator was created, it throws a `ConcurrentModificationException`. This is a safety net, not a guarantee -- it catches bugs early but is not reliable enough for concurrent access across threads.

Approaches to handling this:

| Strategy | How it works | Tradeoff |
|---|---|---|
| **Fail-fast** (Java default) | Collection tracks a modification count; iterator checks it on each call. | Catches bugs but does not prevent them. Not thread-safe. |
| **Snapshot / copy-on-iterate** | Iterator works over a copy of the data taken at creation time. | Safe, but memory-expensive for large collections. |
| **Copy-on-write** (`CopyOnWriteArrayList`) | Writes copy the entire backing array; existing iterators continue over the old snapshot. | Good for read-heavy workloads; writes are expensive. |
| **Concurrent collections** (`ConcurrentHashMap`) | Weakly consistent iterators that tolerate concurrent modification. | No exceptions, but may reflect some mutations and not others. |

In the paginated example above, this issue is less severe because each page fetch is a fresh remote call. But if the server-side data changes between pages, the iterator might skip or duplicate records -- a real problem in production pagination that is often solved with cursor-based (keyset) pagination rather than offset-based.

### Iterator vs Java Streams

Java 8 introduced Streams, which overlap significantly with iterators. How do they compare?

| Concern | Iterator | Stream |
|---|---|---|
| **Control flow** | External (the client calls `next()` in a loop). | Internal (you declare operations; the stream drives execution). |
| **Reuse** | An iterator is consumed once but you can create new ones from the Iterable. | A stream is also single-use. Once a terminal operation runs, it is done. |
| **Lazy evaluation** | Yes -- `next()` computes one element at a time. | Yes -- intermediate operations are lazy; only terminal ops trigger computation. |
| **Rich operations** | None built-in. You write `if`/`while` logic yourself. | `filter`, `map`, `reduce`, `collect`, `flatMap`, etc. out of the box. |
| **Parallelism** | Manual. You manage threads yourself. | `parallelStream()` provides fork-join parallelism with one method call. |
| **Stateful mid-traversal logic** | Easy. The client controls the loop and can break, continue, or carry mutable state. | Awkward. Streams discourage side effects and mutable state. `break` is not available; you use `takeWhile` or `findFirst`. |
| **Integration** | Any `Iterable` works in for-each loops and can be converted to a stream via `StreamSupport`. | Streams are created from collections, arrays, generators, or custom `Spliterator`s. |

**Rule of thumb:** use Streams when you have a transformation pipeline (filter-map-reduce). Use an explicit Iterator (or for-each loop over an Iterable) when you need fine-grained control over the traversal -- early termination based on complex conditions, interleaving reads from multiple iterators, or carrying non-trivial mutable state between iterations.

The two are complementary, not competing. In fact, any `Iterable` can be adapted into a `Stream`:

```java
import java.util.stream.StreamSupport;

StreamSupport.stream(inbox.spliterator(), false)
    .filter(n -> n.sender().equals("Alice"))
    .forEach(System.out::println);
```

### Relationship to other patterns

- **Composite + Iterator.** Iterators are often used to flatten a Composite tree into a linear sequence. An external iterator over a Composite is sometimes called a "tree iterator."
- **Factory Method.** The `createIterator()` / `iterator()` method on the Aggregate is a Factory Method -- the Aggregate decides which ConcreteIterator to instantiate.
- **Memento.** An iterator's internal state (current position, page offset, cursor token) is conceptually a Memento of the traversal. Some designs allow capturing and restoring iterator state.
- **Visitor.** Both Iterator and Visitor provide ways to process elements of a structure. Iterator gives the *client* control over traversal; Visitor inverts it, letting the structure drive the walk and calling back into the visitor at each node.
