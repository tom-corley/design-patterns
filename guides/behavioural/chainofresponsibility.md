# Chain of Responsibility Pattern

**Classification:** Behavioural (GoF)

---

## 1. Core Idea

The Chain of Responsibility pattern decouples the **sender** of a request from its **receiver** by giving more than one object a chance to handle the request. The receiving objects are linked into a chain, and the request is passed along the chain until some object handles it -- or the chain is exhausted.

### The Problem

In many systems a request needs to be processed, but the sender should not know (or care) which specific object will handle it. Hard-wiring the sender to a particular handler creates tight coupling: the sender must know the handler's concrete type, the handler's availability, and its position in the processing order. Any change to the handling logic forces changes in the sender.

Consider an expense-approval workflow. A junior manager can approve expenses up to 1,000. A director can approve up to 10,000. The CFO can approve anything above that. An employee submitting an expense should not need to know *which* approver will handle their claim -- they just submit it, and the system routes it to the right level.

### The Key Insight

Instead of the client choosing the handler, each handler in the chain either processes the request or forwards it to the **next** handler. The client only needs a reference to the head of the chain. The chain itself encapsulates the dispatching logic, and handlers can be added, removed, or reordered without any change to the client.

This is fundamentally about **separation of concerns**: the sender's job is to emit a request; the chain's job is to figure out who should deal with it.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Handler** | Declares the interface for handling requests. Optionally defines a method for setting the next handler in the chain. Often implemented as an abstract class that holds the `next` reference and provides default forwarding behaviour. |
| **ConcreteHandler** | Handles the requests it is responsible for. If it cannot handle a request, it forwards the request to its successor. Each ConcreteHandler knows only about its own competence and its successor -- nothing else. |
| **Client** | Initiates the request by sending it to the first handler in the chain. The client does not know (and should not know) which handler will ultimately process it. |

### How the chain is formed

```
Client --> [HandlerA] --> [HandlerB] --> [HandlerC] --> (end of chain / null)
```

1. The client creates the concrete handlers.
2. The client (or a configuration/factory object) links them together, typically via a `setNext()` method, forming a singly-linked list.
3. The client sends the request to the head of the chain.
4. Each handler inspects the request. If it can handle it, it does so (and may or may not stop propagation). If it cannot, it delegates to the next handler.
5. If no handler processes the request, the chain either silently ignores it or throws an exception, depending on the design.

### Class diagram (textual)

```
         <<interface>>
           Handler
  +-----------------------+
  | + setNext(Handler)    |
  | + handle(Request)     |
  +-----------------------+
            ^
            |
  +-----------------------+       next        +-----------------------+
  |  ConcreteHandlerA     |  ------------->   |  ConcreteHandlerB     |
  +-----------------------+                   +-----------------------+
  | + handle(Request)     |                   | + handle(Request)     |
  +-----------------------+                   +-----------------------+
```

A common implementation detail: the abstract base handler provides a default `handle()` that simply forwards to `next`. Concrete handlers override this, adding their own logic before (or instead of) delegating.

---

## 3. Use Cases

### Middleware pipelines (web frameworks)

This is perhaps the most ubiquitous modern use of the pattern. In frameworks like Express.js, Spring MVC, or Jakarta Servlet Filters, each middleware/filter is a handler in a chain. A request passes through authentication, logging, rate-limiting, CORS, and body-parsing handlers before reaching the actual route handler. Each middleware decides whether to pass the request on (`next()`) or short-circuit the chain (e.g., return a 401).

### Event handling in UI toolkits

In GUI frameworks (Swing, JavaFX, browser DOM), events bubble up through a hierarchy of components. A click on a button first gives the button a chance to handle it. If unhandled, it propagates to the button's container, then to the panel, then to the window. Each component in the hierarchy is a handler in the chain.

### Approval / escalation workflows

Business processes where a request must be approved at the appropriate authority level. A purchase order might be auto-approved below a threshold, require a manager's sign-off at a mid-range, and need executive approval above that. Each level in the hierarchy is a handler.

### Logging frameworks

Log4j and SLF4J route log records through a chain of appenders and filters. A log record passes through each filter, which decides whether to accept, deny, or remain neutral. The chain determines where and whether the record is ultimately written.

### Servlet Filters (Jakarta EE)

The `javax.servlet.FilterChain` is a textbook Chain of Responsibility. Each `Filter` performs pre-processing, calls `chain.doFilter(request, response)` to pass control to the next filter (or the servlet itself), and then optionally performs post-processing. Filters are declared in `web.xml` or via annotations and are composed into a chain by the container.

### Exception handling

Some systems route exceptions through a chain of exception handlers. Each handler checks whether it knows how to deal with a particular exception type. If not, it passes the exception along. Spring's `HandlerExceptionResolver` chain works this way.

---

## 4. Example in Java

The following example models an **expense approval chain** in a company. There are three levels of approver, each with a spending limit. If an expense exceeds all limits, it is rejected.

### The request object

```java
public class ExpenseRequest {

    private final String description;
    private final double amount;
    private final String submittedBy;

    public ExpenseRequest(String description, double amount, String submittedBy) {
        this.description = description;
        this.amount = amount;
        this.submittedBy = submittedBy;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    @Override
    public String toString() {
        return String.format("ExpenseRequest{description='%s', amount=%.2f, submittedBy='%s'}",
                description, amount, submittedBy);
    }
}
```

### The abstract handler

```java
public abstract class Approver {

    private final String name;
    private final String title;
    private Approver next;

    protected Approver(String name, String title) {
        this.name = name;
        this.title = title;
    }

    /**
     * Links the next approver in the chain and returns it,
     * enabling a fluent chaining style.
     */
    public Approver setNext(Approver next) {
        this.next = next;
        return next;
    }

    /**
     * Template for handling an expense request.
     * Concrete subclasses define the approval limit.
     */
    public void handle(ExpenseRequest request) {
        if (canApprove(request)) {
            approve(request);
        } else if (next != null) {
            System.out.printf("  %s (%s): amount %.2f exceeds my limit -- forwarding.%n",
                    name, title, request.getAmount());
            next.handle(request);
        } else {
            reject(request);
        }
    }

    protected abstract boolean canApprove(ExpenseRequest request);

    private void approve(ExpenseRequest request) {
        System.out.printf("  %s (%s): APPROVED '%s' for %.2f%n",
                name, title, request.getDescription(), request.getAmount());
    }

    private void reject(ExpenseRequest request) {
        System.out.printf("  %s (%s): REJECTED '%s' for %.2f -- exceeds all approval limits%n",
                name, title, request.getDescription(), request.getAmount());
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }
}
```

### Concrete handlers

```java
public class TeamLead extends Approver {

    private static final double LIMIT = 1_000.0;

    public TeamLead(String name) {
        super(name, "Team Lead");
    }

    @Override
    protected boolean canApprove(ExpenseRequest request) {
        return request.getAmount() <= LIMIT;
    }
}
```

```java
public class Director extends Approver {

    private static final double LIMIT = 10_000.0;

    public Director(String name) {
        super(name, "Director");
    }

    @Override
    protected boolean canApprove(ExpenseRequest request) {
        return request.getAmount() <= LIMIT;
    }
}
```

```java
public class CFO extends Approver {

    private static final double LIMIT = 100_000.0;

    public CFO(String name) {
        super(name, "CFO");
    }

    @Override
    protected boolean canApprove(ExpenseRequest request) {
        return request.getAmount() <= LIMIT;
    }
}
```

### The client

```java
public class ExpenseApprovalDemo {

    public static void main(String[] args) {
        // Build the chain: TeamLead -> Director -> CFO
        Approver teamLead = new TeamLead("Alice");
        Approver director = new Director("Bob");
        Approver cfo = new CFO("Carol");

        teamLead.setNext(director).setNext(cfo);

        // Submit various expense requests
        ExpenseRequest[] requests = {
            new ExpenseRequest("Team lunch", 250.0, "Dave"),
            new ExpenseRequest("Conference tickets", 4_500.0, "Eve"),
            new ExpenseRequest("Server infrastructure", 45_000.0, "Frank"),
            new ExpenseRequest("Office building lease", 500_000.0, "Grace"),
        };

        for (ExpenseRequest request : requests) {
            System.out.printf("Processing: %s%n", request);
            teamLead.handle(request);
            System.out.println();
        }
    }
}
```

### Expected output

```
Processing: ExpenseRequest{description='Team lunch', amount=250.00, submittedBy='Dave'}
  Alice (Team Lead): APPROVED 'Team lunch' for 250.00

Processing: ExpenseRequest{description='Conference tickets', amount=4500.00, submittedBy='Eve'}
  Alice (Team Lead): amount 4500.00 exceeds my limit -- forwarding.
  Bob (Director): APPROVED 'Conference tickets' for 4500.00

Processing: ExpenseRequest{description='Server infrastructure', amount=45000.00, submittedBy='Frank'}
  Alice (Team Lead): amount 45000.00 exceeds my limit -- forwarding.
  Bob (Director): amount 45000.00 exceeds my limit -- forwarding.
  Carol (CFO): APPROVED 'Server infrastructure' for 45000.00

Processing: ExpenseRequest{description='Office building lease', amount=500000.00, submittedBy='Grace'}
  Alice (Team Lead): amount 500000.00 exceeds my limit -- forwarding.
  Bob (Director): amount 500000.00 exceeds my limit -- forwarding.
  Carol (CFO): REJECTED 'Office building lease' for 500000.00 -- exceeds all approval limits
```

### What to observe

- The client (`main`) only references the head of the chain (`teamLead`). It has no knowledge of the Director or CFO.
- Handlers are loosely coupled: you can insert a new `VicePresident` handler between Director and CFO without modifying any existing handler or the client.
- The chain handles the "no one can approve" case explicitly at the tail, by rejecting.

---

## 5. Tradeoffs and Limitations

### When to use it

- **The set of handlers is dynamic or configurable.** If the processing pipeline needs to change at runtime (add a handler, remove one, reorder), the chain makes this straightforward.
- **The sender should not know the receiver.** The pattern shines when the client should be decoupled from the specifics of request processing.
- **Multiple handlers might need to inspect the request.** In the middleware variant, every handler gets a chance to act (logging, auth, compression) rather than only one handler processing the request.
- **You want to avoid large conditional dispatching.** A long `if/else if` chain in the client selecting a handler is a code smell that this pattern can eliminate.

### When NOT to use it

- **Every request has a known, single handler.** If the mapping from request to handler is fixed and straightforward (e.g., a simple map lookup), the chain adds unnecessary indirection. A `Map<RequestType, Handler>` or the Command pattern may be simpler.
- **Performance is critical and the chain is long.** Each request traverses the chain linearly. If the chain has many handlers and requests are frequent, the overhead may matter. In practice this is rarely a bottleneck, but it is worth considering.
- **You need a guaranteed response.** The basic pattern does not guarantee that any handler will process the request. You need to deliberately design for the "unhandled" case (a fallback handler at the end, or throwing an exception).

### Unhandled requests

The most common pitfall. If the chain is misconfigured or a request type is not anticipated, the request silently falls off the end of the chain. Strategies to mitigate this:

- **Tail sentinel:** Place a catch-all handler at the end that either throws an exception or logs a warning.
- **Return value:** Have `handle()` return a boolean (or an `Optional<Result>`) so the client can detect whether the request was processed.
- **Null Object pattern:** The last handler in the chain is a no-op handler that always "handles" the request with a default or error behaviour.

### Debugging difficulty

When a request passes through many handlers, it can be hard to trace which handler acted (or failed to act). Each handler only knows about itself and its successor, so there is no single place that shows the full chain. Mitigations:

- Add logging at each handler's entry and exit.
- Provide a method that walks the chain and prints its structure for diagnostics.
- In testing, verify each handler independently and also test the assembled chain end-to-end.

### Ordering sensitivity

The order of handlers in the chain matters. An authentication handler must come before an authorization handler. A decompression filter must come before a JSON-parsing filter. Getting the order wrong produces subtle bugs. Document the expected order and, if possible, enforce it programmatically.

### Comparison with related patterns

| Pattern | Relationship |
|---|---|
| **Command** | Command encapsulates a request as an object. Chain of Responsibility routes that request through a series of potential handlers. The two are often combined. |
| **Decorator** | Structurally similar (both form linked chains), but Decorator adds behaviour to an object, while Chain of Responsibility dispatches to one of several alternative handlers. |
| **Composite** | In UI frameworks, event bubbling through a Composite tree is essentially a Chain of Responsibility over a tree structure rather than a linear list. |
| **Mediator** | Mediator centralises communication; Chain of Responsibility distributes it. If you want a single coordinator that knows all the handlers, use Mediator. If you want handlers to be self-organising, use Chain of Responsibility. |
