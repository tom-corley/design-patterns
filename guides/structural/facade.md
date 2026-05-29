# Facade Pattern

**Classification:** Structural (GoF)

---

## 1. Core Idea

The Facade pattern provides a unified, simplified interface to a set of interfaces
in a subsystem. It defines a higher-level interface that makes the subsystem easier
to use without hiding it entirely.

### The Problem

Complex subsystems accumulate many classes over time. Using the subsystem correctly
requires understanding which objects to create, in what order to call them, and how
they depend on each other. Client code becomes tightly coupled to implementation
details it should not need to know about.

Consider an order fulfilment pipeline: the client must check inventory, authorise
payment, generate an invoice, arrange shipping, and send a confirmation -- each
handled by a separate service with its own interface. Without a facade, every caller
must orchestrate this entire sequence and handle partial-failure rollback.

### The Key Insight

You do not need to wrap or replace the subsystem. You only need to place a
convenient entry point in front of it. Clients that need the simple, common
workflow call the facade. Clients that need fine-grained control can still reach
past it and use the subsystem classes directly. The facade does not encapsulate the
subsystem; it merely *fronts* it.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Facade** | Knows which subsystem classes are responsible for a request. Delegates client requests to appropriate subsystem objects. Contains no business logic of its own -- only orchestration. |
| **Subsystem classes** | Implement subsystem functionality. Handle work assigned by the Facade. Have no knowledge of the Facade; they do not hold references to it. |
| **Client** | Calls the Facade instead of calling subsystem objects directly. May still access subsystem classes when it needs behaviour the Facade does not expose. |

### How Delegation Works

```
Client
  |
  v
Facade.placeOrder(order)
  |--- InventoryService.reserve(items)
  |--- PaymentService.authorise(card, amount)
  |--- InvoiceService.generate(order, paymentRef)
  |--- ShippingService.dispatch(order, warehouse)
  |--- NotificationService.sendConfirmation(order)
```

The facade composes calls to subsystem objects in the correct order, passes return
values between steps where needed, and presents a single result to the client. Each
subsystem class is independent and unaware of the facade.

### Class Diagram (textual)

```
+------------------+
|     Client       |
+------------------+
        |
        v
+------------------+         +--------------------+
|   OrderFacade    |-------->| InventoryService   |
|                  |-------->| PaymentService     |
|  + placeOrder()  |-------->| InvoiceService     |
|  + cancelOrder() |-------->| ShippingService    |
|                  |-------->| NotificationService|
+------------------+         +--------------------+
```

---

## 3. Use Cases

### Compiler Subsystem

A compiler has distinct phases: lexical analysis, parsing, semantic analysis, code
generation, and optimisation. A `CompilerFacade.compile(sourceFile)` method lets
tool authors invoke the full pipeline in one call while compiler engineers still
access individual phases for debugging or extension.

### Order Processing / E-Commerce

An order fulfilment system coordinates inventory checks, payment authorisation,
fraud screening, invoice generation, warehouse dispatch, and email confirmation.
A facade exposes `placeOrder()` and `cancelOrder()` so that web controllers and
API handlers do not need to know about the internal services.

### Video Conversion

Converting a video involves demuxing the container, decoding audio and video
streams, applying filters, re-encoding into a target codec, and muxing into a new
container. A `VideoConverterFacade.convert(input, outputFormat)` hides the codec
libraries, filter chains, and container formats behind one method.

### Report Generation

Generating a business report may require querying multiple data sources, running
aggregations, formatting tables, rendering charts, and exporting to PDF. A
`ReportFacade.generate(reportType, dateRange)` orchestrates these steps.

### Home Automation

A "good night" routine dims lights, locks doors, sets the thermostat, and arms the
alarm. A `SmartHomeFacade.goodnightMode()` coordinates the individual device APIs.

---

## 4. Example in Java

This example models an **order fulfilment system**. Five subsystem services each
own one responsibility. The `OrderFacade` orchestrates them into a single
`placeOrder` operation and handles rollback if a downstream step fails.

### Subsystem Classes

```java
// ---- InventoryService.java ----

public class InventoryService {

    public boolean reserve(String itemId, int quantity) {
        System.out.println("[Inventory] Reserving " + quantity + "x " + itemId);
        // In a real system: check stock, decrement available count, return success/failure
        return true;
    }

    public void release(String itemId, int quantity) {
        System.out.println("[Inventory] Releasing " + quantity + "x " + itemId);
    }
}
```

```java
// ---- PaymentService.java ----

public class PaymentService {

    public String authorise(String cardToken, double amount) {
        System.out.println("[Payment] Authorising " + amount + " on card " + cardToken);
        // Returns a payment reference on success, null on failure
        return "PAY-" + System.currentTimeMillis();
    }

    public void refund(String paymentRef) {
        System.out.println("[Payment] Refunding " + paymentRef);
    }
}
```

```java
// ---- InvoiceService.java ----

public class InvoiceService {

    public String generate(String orderId, String paymentRef, double amount) {
        String invoiceId = "INV-" + orderId;
        System.out.println("[Invoice] Generated " + invoiceId
                + " for payment " + paymentRef + ", amount " + amount);
        return invoiceId;
    }
}
```

```java
// ---- ShippingService.java ----

public class ShippingService {

    public String dispatch(String orderId, String itemId, int quantity, String address) {
        String trackingId = "TRACK-" + orderId;
        System.out.println("[Shipping] Dispatching " + quantity + "x " + itemId
                + " to " + address + " (tracking: " + trackingId + ")");
        return trackingId;
    }
}
```

```java
// ---- NotificationService.java ----

public class NotificationService {

    public void sendConfirmation(String email, String orderId, String trackingId) {
        System.out.println("[Notification] Sending confirmation to " + email
                + " for order " + orderId + " (tracking: " + trackingId + ")");
    }
}
```

### The Facade

```java
// ---- OrderFacade.java ----

public class OrderFacade {

    private final InventoryService inventory;
    private final PaymentService payment;
    private final InvoiceService invoices;
    private final ShippingService shipping;
    private final NotificationService notifications;

    public OrderFacade() {
        this.inventory     = new InventoryService();
        this.payment       = new PaymentService();
        this.invoices      = new InvoiceService();
        this.shipping      = new ShippingService();
        this.notifications = new NotificationService();
    }

    /**
     * Orchestrates the full order-placement workflow.
     * Returns the tracking ID on success, or throws on failure.
     */
    public String placeOrder(String orderId,
                             String itemId,
                             int quantity,
                             double unitPrice,
                             String cardToken,
                             String shippingAddress,
                             String customerEmail) {

        double totalAmount = quantity * unitPrice;

        // Step 1: Reserve inventory
        boolean reserved = inventory.reserve(itemId, quantity);
        if (!reserved) {
            throw new IllegalStateException("Item " + itemId + " is out of stock");
        }

        // Step 2: Authorise payment
        String paymentRef = payment.authorise(cardToken, totalAmount);
        if (paymentRef == null) {
            inventory.release(itemId, quantity);  // rollback step 1
            throw new IllegalStateException("Payment declined for order " + orderId);
        }

        // Step 3: Generate invoice
        String invoiceId = invoices.generate(orderId, paymentRef, totalAmount);

        // Step 4: Dispatch shipment
        String trackingId = shipping.dispatch(orderId, itemId, quantity, shippingAddress);

        // Step 5: Notify customer
        notifications.sendConfirmation(customerEmail, orderId, trackingId);

        System.out.println("[OrderFacade] Order " + orderId + " placed successfully"
                + " | invoice: " + invoiceId + " | tracking: " + trackingId);

        return trackingId;
    }
}
```

### Client Code

```java
// ---- Main.java ----

public class Main {

    public static void main(String[] args) {
        OrderFacade facade = new OrderFacade();

        String trackingId = facade.placeOrder(
                "ORD-1001",        // orderId
                "SKU-KEYBOARD",    // itemId
                2,                 // quantity
                49.99,             // unit price
                "tok_visa_4242",   // card token
                "42 Java Lane, London, UK",  // shipping address
                "buyer@example.com"          // customer email
        );

        System.out.println("\nOrder complete. Tracking: " + trackingId);
    }
}
```

### Expected Output

```
[Inventory] Reserving 2x SKU-KEYBOARD
[Payment] Authorising 99.98 on card tok_visa_4242
[Invoice] Generated INV-ORD-1001 for payment PAY-1716940800000, amount 99.98
[Shipping] Dispatching 2x SKU-KEYBOARD to 42 Java Lane, London, UK (tracking: TRACK-ORD-1001)
[Notification] Sending confirmation to buyer@example.com for order ORD-1001 (tracking: TRACK-ORD-1001)
[OrderFacade] Order ORD-1001 placed successfully | invoice: INV-ORD-1001 | tracking: TRACK-ORD-1001

Order complete. Tracking: TRACK-ORD-1001
```

### Design Notes

- **No business logic in the facade.** Each decision (is the item in stock? is the
  payment valid?) lives in the subsystem service. The facade only sequences calls
  and routes data between them.
- **Rollback on failure.** When payment fails, the facade releases the inventory
  reservation. This orchestration logic is the facade's main value -- clients do
  not need to remember the rollback steps.
- **Subsystem classes are independent.** `InventoryService` knows nothing about
  `PaymentService`. They can be tested, reused, and evolved independently.
- **The subsystem is still accessible.** A client that only needs to check stock
  can call `InventoryService.reserve()` directly; the facade does not prevent this.

---

## 5. Tradeoffs and Limitations

### When to Use Facade

- You have a subsystem with many classes and a common high-level workflow that most
  clients need.
- You want to decouple client code from subsystem internals so that internal
  refactoring does not ripple outward.
- You are wrapping a third-party library or legacy module and want a simpler
  interface tailored to your application's needs.
- You are building a layered architecture and need a clear entry point for each
  layer.

### When Not to Use Facade

- The subsystem is already simple -- adding a facade just adds an indirection layer
  with no benefit.
- Every client needs a different workflow through the subsystem. If there is no
  "common path," the facade either grows many methods (becoming a god object) or
  clients bypass it entirely.
- You need the facade to enforce access control, preventing clients from reaching
  subsystem classes. Facade does not hide the subsystem; it only provides a
  convenient alternative entry point. If you need strict encapsulation, consider
  using module boundaries or access modifiers instead.

### The God Object Risk

A facade that accumulates too many methods or too much orchestration logic becomes
a god object -- a single class that knows about everything and changes for every
reason. Guard against this by:

- Keeping the facade thin: orchestration only, no business rules.
- Splitting into multiple focused facades if the subsystem has distinct use-case
  groups (e.g. `OrderFacade`, `ReturnsFacade`, `ReportingFacade`).
- Treating a growing facade as a design smell that the subsystem itself may need
  restructuring.

### Facade vs Mediator

These patterns are often confused because both sit between a set of objects and
coordinate interaction, but their intent is different:

| | Facade | Mediator |
|---|---|---|
| **Direction** | One-directional: client calls facade, facade calls subsystem. Subsystem classes do not know the facade exists. | Bidirectional: colleagues communicate *through* the mediator, and the mediator calls back into colleagues. |
| **Purpose** | Simplify access to a subsystem for external clients. | Manage complex inter-object communication *within* a group of peers. |
| **Coupling** | Reduces coupling between client and subsystem. Subsystem classes remain independent of each other. | Replaces many-to-many coupling between colleagues with many-to-one coupling to the mediator. |
| **Typical scope** | Wraps an entire subsystem or library. | Coordinates a specific set of collaborating objects (e.g. UI widgets in a dialog). |

A simple test: if the "coordinator" only delegates outward and the subsystem classes
never call back into it, you have a Facade. If communication flows both ways and the
coordinator contains interaction logic between peers, you have a Mediator.

### Facade vs Adapter

Adapter changes the interface of a single existing class to match what a client
expects. Facade does not change interfaces -- it provides a *new*, simplified
interface that delegates to multiple existing classes. Adapter is about
compatibility; Facade is about convenience.

### Summary

Facade is one of the simplest and most useful structural patterns. Its value comes
not from clever mechanics but from disciplined separation of concerns: the facade
owns the "how to orchestrate," subsystem classes own the "how to do," and clients
own the "what to request." The main risk is letting the facade grow beyond
orchestration into a dumping ground for logic that belongs in the subsystem.
