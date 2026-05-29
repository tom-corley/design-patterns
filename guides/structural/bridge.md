# Bridge Pattern

**Classification:** Structural (GoF)

---

## 1. Core Idea

The Bridge pattern decouples an **abstraction** from its **implementation** so that the two can vary independently. It replaces a compile-time binding between an abstraction and its implementation with a runtime binding, using composition instead of inheritance.

### The Problem

Imagine you are building a notification system. You have different **types** of notification (alert, reminder, promotional) and different **delivery channels** (email, SMS, push notification). The naive approach is to create a subclass for every combination: `AlertEmail`, `AlertSMS`, `AlertPush`, `ReminderEmail`, `ReminderSMS`, and so on. This leads to a class explosion -- M types multiplied by N channels gives M x N classes. Adding a new channel means adding a new subclass for every notification type, and vice versa.

### The Key Insight

The combinatorial explosion happens because two independent dimensions of variation (what kind of notification and how it is delivered) are being fused into a single inheritance hierarchy. The Bridge pattern separates them: one hierarchy for the abstraction (notification type) and one for the implementation (delivery channel). The abstraction holds a reference to the implementation and delegates to it. Each dimension can now grow independently without affecting the other.

This is fundamentally about preferring **composition over inheritance** when you have orthogonal axes of variation.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Abstraction** | Defines the high-level interface. Holds a reference to an `Implementor`. Delegates implementation-specific work to it. |
| **RefinedAbstraction** | Extends the `Abstraction` with additional behaviour or specialisation. |
| **Implementor** | Defines the interface for implementation classes. This does not need to mirror the `Abstraction` interface -- it typically provides low-level, primitive operations that the `Abstraction` composes into higher-level ones. |
| **ConcreteImplementor** | Provides a concrete implementation of the `Implementor` interface. |

### Relationships

```
        Abstraction  ─────────────────>  Implementor
            │          (has-a / delegates)       │
            │                                    │
    RefinedAbstraction              ConcreteImplementorA
    RefinedAbstraction2             ConcreteImplementorB
```

- `Abstraction` holds a reference to `Implementor` (composition).
- `RefinedAbstraction` inherits from `Abstraction` and may add domain-specific behaviour.
- `ConcreteImplementor` classes implement the `Implementor` interface independently.
- The client configures which `ConcreteImplementor` to inject into which `Abstraction` at runtime.

The critical design decision is **where the seam goes**. The `Implementor` interface should capture the primitive operations that vary across platforms or channels, while the `Abstraction` interface captures the domain-level operations that clients care about.

---

## 3. Use Cases

### Cross-Platform Rendering

A drawing library defines shapes (`Circle`, `Rectangle`) as the abstraction hierarchy. The rendering backend (OpenGL, DirectX, SVG) is the implementation hierarchy. Each shape delegates its `draw()` call to a renderer. Adding a new shape does not require touching the renderer code, and adding a new renderer does not require touching the shape code.

### Notification System (Multiple Types x Multiple Channels)

Notification types (alert, reminder, promotional) define what content is sent and how urgently. Delivery channels (email, SMS, push) define how the message physically reaches the user. The Bridge lets you add a new notification type or a new channel without modifying the other hierarchy.

### Persistence Layer

A domain model (e.g., `UserRepository`, `OrderRepository`) defines high-level data access operations. The underlying persistence mechanism (SQL database, NoSQL store, in-memory cache) is the implementation. The repository abstraction delegates storage primitives to a `DataStore` implementor.

### Device Drivers

An operating system defines a generic device abstraction (printer, scanner). Manufacturers provide concrete driver implementations. The OS code talks to the abstraction; the driver implements the low-level protocol.

---

## 4. Example in Java

This example models a notification system with two dimensions:
- **Notification type** (abstraction): `AlertNotification`, `ReminderNotification`
- **Delivery channel** (implementor): `EmailChannel`, `SmsChannel`, `PushChannel`

### Implementor -- `DeliveryChannel`

```java
/**
 * The Implementor interface. Defines the primitive operations
 * for delivering a message through a specific channel.
 */
public interface DeliveryChannel {

    /**
     * Send a message to a recipient.
     *
     * @param recipient the target (email address, phone number, device token, etc.)
     * @param subject   a short summary or subject line
     * @param body      the full message content
     */
    void sendMessage(String recipient, String subject, String body);
}
```

### ConcreteImplementors

```java
public class EmailChannel implements DeliveryChannel {

    @Override
    public void sendMessage(String recipient, String subject, String body) {
        System.out.println("[EMAIL] To: " + recipient);
        System.out.println("  Subject: " + subject);
        System.out.println("  Body: " + body);
        System.out.println("  -- sent via SMTP --");
        System.out.println();
    }
}
```

```java
public class SmsChannel implements DeliveryChannel {

    private static final int MAX_SMS_LENGTH = 160;

    @Override
    public void sendMessage(String recipient, String subject, String body) {
        // SMS has no subject line, so we prepend it to the body
        String fullText = subject + ": " + body;
        if (fullText.length() > MAX_SMS_LENGTH) {
            fullText = fullText.substring(0, MAX_SMS_LENGTH - 3) + "...";
        }
        System.out.println("[SMS] To: " + recipient);
        System.out.println("  Message: " + fullText);
        System.out.println("  -- sent via SMS gateway --");
        System.out.println();
    }
}
```

```java
public class PushChannel implements DeliveryChannel {

    @Override
    public void sendMessage(String recipient, String subject, String body) {
        System.out.println("[PUSH] Device: " + recipient);
        System.out.println("  Title: " + subject);
        System.out.println("  Alert: " + body);
        System.out.println("  -- sent via push service --");
        System.out.println();
    }
}
```

### Abstraction -- `Notification`

```java
/**
 * The Abstraction. Defines the high-level notification interface
 * and delegates delivery to a DeliveryChannel implementor.
 */
public abstract class Notification {

    protected final DeliveryChannel channel;

    protected Notification(DeliveryChannel channel) {
        this.channel = channel;
    }

    /**
     * Send this notification to the given recipient.
     * Subclasses define the content; the channel defines the transport.
     */
    public abstract void send(String recipient);
}
```

### RefinedAbstractions

```java
/**
 * An urgent alert notification. Adds priority tagging
 * and formats the message with high-urgency language.
 */
public class AlertNotification extends Notification {

    private final String alertMessage;
    private final AlertSeverity severity;

    public enum AlertSeverity { LOW, MEDIUM, HIGH, CRITICAL }

    public AlertNotification(DeliveryChannel channel, String alertMessage, AlertSeverity severity) {
        super(channel);
        this.alertMessage = alertMessage;
        this.severity = severity;
    }

    @Override
    public void send(String recipient) {
        String subject = "[" + severity.name() + " ALERT]";
        String body = "ALERT (" + severity.name() + "): " + alertMessage
                + " -- Immediate attention may be required.";
        channel.sendMessage(recipient, subject, body);
    }
}
```

```java
/**
 * A scheduled reminder notification. Includes a due date
 * and formats the message as a gentle nudge.
 */
public class ReminderNotification extends Notification {

    private final String task;
    private final String dueDate;

    public ReminderNotification(DeliveryChannel channel, String task, String dueDate) {
        super(channel);
        this.task = task;
        this.dueDate = dueDate;
    }

    @Override
    public void send(String recipient) {
        String subject = "Reminder: " + task;
        String body = "This is a reminder that \"" + task + "\" is due on " + dueDate
                + ". Please make sure it is completed on time.";
        channel.sendMessage(recipient, subject, body);
    }
}
```

### Client Code

```java
public class Main {

    public static void main(String[] args) {
        // Create delivery channels (implementors)
        DeliveryChannel email = new EmailChannel();
        DeliveryChannel sms = new SmsChannel();
        DeliveryChannel push = new PushChannel();

        // Create notifications (abstractions) with different channels
        Notification criticalAlert = new AlertNotification(
                email, "Database connection pool exhausted", AlertNotification.AlertSeverity.CRITICAL);

        Notification mediumAlert = new AlertNotification(
                sms, "Disk usage above 80%", AlertNotification.AlertSeverity.MEDIUM);

        Notification pushReminder = new ReminderNotification(
                push, "Submit quarterly report", "2026-06-01");

        Notification emailReminder = new ReminderNotification(
                email, "Renew SSL certificate", "2026-06-15");

        // Send them -- each notification delegates to its channel
        criticalAlert.send("ops-team@example.com");
        mediumAlert.send("+44-7700-900123");
        pushReminder.send("device-token-abc123");
        emailReminder.send("admin@example.com");

        // The bridge in action: same notification type, different channel,
        // or same channel, different notification type -- no new classes needed.
        System.out.println("=== Same alert, different channels ===");
        String alertMsg = "CPU temperature exceeding threshold";
        new AlertNotification(email, alertMsg, AlertNotification.AlertSeverity.HIGH)
                .send("ops-team@example.com");
        new AlertNotification(sms, alertMsg, AlertNotification.AlertSeverity.HIGH)
                .send("+44-7700-900456");
        new AlertNotification(push, alertMsg, AlertNotification.AlertSeverity.HIGH)
                .send("device-token-xyz789");
    }
}
```

### Expected Output

```
[EMAIL] To: ops-team@example.com
  Subject: [CRITICAL ALERT]
  Body: ALERT (CRITICAL): Database connection pool exhausted -- Immediate attention may be required.
  -- sent via SMTP --

[SMS] To: +44-7700-900123
  Message: [MEDIUM ALERT]: ALERT (MEDIUM): Disk usage above 80% -- Immediate attention may be required.
  -- sent via SMS gateway --

[PUSH] Device: device-token-abc123
  Title: Reminder: Submit quarterly report
  Alert: This is a reminder that "Submit quarterly report" is due on 2026-06-01. Please make sure it is completed on time.
  -- sent via push service --

[EMAIL] To: admin@example.com
  Subject: Reminder: Renew SSL certificate
  Body: This is a reminder that "Renew SSL certificate" is due on 2026-06-15. Please make sure it is completed on time.
  -- sent via SMTP --

=== Same alert, different channels ===
[EMAIL] To: ops-team@example.com
  Subject: [HIGH ALERT]
  Body: ALERT (HIGH): CPU temperature exceeding threshold -- Immediate attention may be required.
  -- sent via SMTP --

[SMS] To: +44-7700-900456
  Message: [HIGH ALERT]: ALERT (HIGH): CPU temperature exceeding threshold -- Immediate attention may be required...
  -- sent via SMS gateway --

[PUSH] Device: device-token-xyz789
  Title: [HIGH ALERT]
  Alert: ALERT (HIGH): CPU temperature exceeding threshold -- Immediate attention may be required.
  -- sent via push service --
```

### Why This Works

Without the Bridge, supporting 2 notification types and 3 channels would require 6 concrete classes (`AlertEmail`, `AlertSms`, `AlertPush`, `ReminderEmail`, `ReminderSms`, `ReminderPush`). Adding a fourth channel (e.g., Slack webhook) would mean adding 2 more classes. Adding a third notification type would mean adding 3 more.

With the Bridge, adding a channel means writing one new `DeliveryChannel` implementation. Adding a notification type means writing one new `Notification` subclass. The growth is M + N instead of M x N.

---

## 5. Tradeoffs and Limitations

### When to Use It

- **Two independent dimensions of variation.** If you find yourself building (or heading towards) a combinatorial class hierarchy where one axis is "what kind of thing" and the other is "how it does its thing", the Bridge is the right tool.
- **You need to switch implementations at runtime.** Because the implementor is injected via composition, you can swap it without changing the abstraction. This is useful for A/B testing delivery channels, switching rendering backends, or selecting a persistence layer based on configuration.
- **You want to hide implementation details from clients.** Clients program to the `Abstraction` interface and never see the `Implementor` hierarchy.

### When Not to Use It

- **Only one dimension varies.** If you have multiple notification types but only one delivery channel (and will never add more), the extra indirection adds complexity for no benefit. A simple inheritance hierarchy or even a few concrete classes will do.
- **The abstraction and implementation are tightly coupled by nature.** If every change to the abstraction requires a corresponding change to the implementor interface, you do not actually have two independent hierarchies. The Bridge will just shuffle code around without decoupling anything.
- **Premature generalisation.** Do not introduce a Bridge "just in case" a second dimension appears. Wait until you have concrete evidence of a second axis of variation. Refactoring to a Bridge later is straightforward; carrying unnecessary abstraction layers from the start is a maintenance tax.

### Complexity Cost

The Bridge adds at least four types (Abstraction, RefinedAbstraction, Implementor, ConcreteImplementor) where a direct approach might use one or two. Each call from abstraction to implementation is an extra layer of indirection. For small systems this overhead is not justified. For systems with genuine multi-dimensional variation, the cost pays for itself quickly.

### Bridge vs Strategy

These two patterns are structurally almost identical -- both use composition to delegate to an interface. The difference is in **intent**:

| | Bridge | Strategy |
|---|---|---|
| **Purpose** | Decouple an abstraction from its implementation so both hierarchies can evolve independently | Allow a client to choose among interchangeable algorithms at runtime |
| **Number of hierarchies** | Two parallel hierarchies (abstraction + implementation) | One hierarchy (the strategy family); the context is typically a single class, not a hierarchy |
| **Granularity** | The implementor often provides a broad interface with multiple primitives that the abstraction orchestrates | A strategy typically encapsulates a single algorithm or behaviour |
| **When it emerges** | At design time, when you recognise two orthogonal dimensions | At refactoring time, when you extract a varying algorithm from a class |

In practice, if you have a family of related abstractions that all share a pluggable implementation dimension, you have a Bridge. If you have a single class that needs to swap out one algorithm, you have a Strategy. The line can be blurry, and that is fine -- the structural mechanics are the same.
