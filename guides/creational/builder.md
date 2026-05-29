# Builder Pattern

**Classification:** Creational (GoF)

---

## 1. Core Idea

The Builder pattern separates the **construction** of a complex object from its **representation**, so that the same construction process can create different representations.

### The Problem

Some objects are expensive or complicated to construct. They have many fields -- some required, some optional -- and the valid combinations are not always obvious. You end up with one of two bad outcomes:

- **Telescoping constructors:** A family of constructors with increasing parameter counts. The call site becomes unreadable, parameter ordering is fragile, and adding a new optional field means yet another overload.
- **JavaBean-style setters:** A no-arg constructor followed by a sequence of `setFoo()` calls. The object is mutable and exists in an incomplete state between construction and first use. You cannot enforce invariants at construction time.

### The Key Insight

Extract the step-by-step construction logic into a dedicated **Builder** object. The builder accumulates configuration, validates it, and then produces the finished product in a single terminal `build()` call. The product itself can be immutable, and construction can be validated before the object ever exists.

A secondary insight from the GoF formulation is that you can define an **abstract Builder interface** and swap in different concrete builders to produce different products from the same construction steps, orchestrated by a **Director**.

---

## 2. Structure

The classic GoF Builder has four participants:

| Participant | Role |
|---|---|
| **Product** | The complex object being constructed. |
| **Builder** (interface) | Declares the step methods for constructing parts of the Product. |
| **ConcreteBuilder** | Implements the Builder interface; assembles and tracks parts; provides a method to retrieve the finished Product. |
| **Director** | Orchestrates the build steps in a particular order using a Builder reference. The Director knows the *recipe* but not the *representation*. |

### Relationships

```
  Director
     |
     | uses
     v
  <<interface>>
    Builder          ------>  Product
     ^
     |
  ConcreteBuilder
```

1. The **Director** calls builder methods in a specific sequence.
2. The **ConcreteBuilder** handles each call by accumulating state internally.
3. The client retrieves the **Product** from the ConcreteBuilder once the Director finishes.

### Simplified (Modern) Variant

In practice, many Java codebases skip the Director and the abstract Builder interface entirely. The builder is a static inner class of the product, and the client drives the steps directly via a fluent API. This is the form popularised by Josh Bloch in *Effective Java* (Item 2). Both forms are valid; the GoF form is more useful when you need polymorphic construction, while the Bloch form is more common for everyday immutable-object construction.

---

## 3. Use Cases

**Objects with many optional parameters.** Any class where the constructor would need four or more parameters, most of them optional, is a strong candidate. Examples: configuration objects, HTTP requests, database connection settings.

**Immutable objects that are expensive to validate.** When you want the finished object to be immutable but need to accumulate state over multiple steps before you can validate and freeze it.

**Constructing different representations from the same input.** The GoF Director form shines here. A document parser (the Director) can drive a Builder interface to produce either an HTML document, a PDF document, or a plain-text document from the same parsing logic.

**Fluent DSLs and query construction.** SQL query builders, HTTP request builders, and test-data builders all use the pattern to provide a readable, chainable API.

**Test fixtures.** Builders make it easy to create objects with sensible defaults and override only the fields relevant to a particular test case, avoiding brittle constructors in test code.

---

## 4. Example in Java

This example models an HTTP request builder -- a realistic domain where requests have a required method and URL, plus many optional components (headers, query parameters, body, timeouts).

### Product: `HttpRequest`

```java
package creational.builder.http;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An immutable HTTP request object. Cannot be constructed directly --
 * clients must use HttpRequest.Builder.
 */
public final class HttpRequest {

    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final String body;
    private final int timeoutMillis;

    private HttpRequest(Builder builder) {
        this.method = builder.method;
        this.url = builder.url;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
        this.queryParams = Collections.unmodifiableMap(new LinkedHashMap<>(builder.queryParams));
        this.body = builder.body;
        this.timeoutMillis = builder.timeoutMillis;
    }

    public String getMethod()                { return method; }
    public String getUrl()                   { return url; }
    public Map<String, String> getHeaders()  { return headers; }
    public Map<String, String> getQueryParams() { return queryParams; }
    public String getBody()                  { return body; }
    public int getTimeoutMillis()            { return timeoutMillis; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(url);
        if (!queryParams.isEmpty()) {
            sb.append("?");
            queryParams.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
            sb.setLength(sb.length() - 1); // trim trailing &
        }
        sb.append("\n");
        headers.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
        if (body != null) {
            sb.append("\n").append(body).append("\n");
        }
        sb.append("[timeout=").append(timeoutMillis).append("ms]");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Builder (static inner class -- the Bloch style)
    // ---------------------------------------------------------------

    public static class Builder {

        // Required parameters
        private final String method;
        private final String url;

        // Optional parameters -- initialised to sensible defaults
        private final Map<String, String> headers = new LinkedHashMap<>();
        private final Map<String, String> queryParams = new LinkedHashMap<>();
        private String body;
        private int timeoutMillis = 30_000; // default 30 seconds

        /**
         * @param method HTTP method (GET, POST, PUT, DELETE, etc.)
         * @param url    target URL
         */
        public Builder(String method, String url) {
            if (method == null || method.isBlank()) {
                throw new IllegalArgumentException("HTTP method must not be blank");
            }
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("URL must not be blank");
            }
            this.method = method.toUpperCase();
            this.url = url;
        }

        public Builder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Builder queryParam(String name, String value) {
            queryParams.put(name, value);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder timeoutMillis(int timeoutMillis) {
            if (timeoutMillis <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        /**
         * Terminal operation. Validates accumulated state and returns
         * an immutable HttpRequest.
         */
        public HttpRequest build() {
            // Cross-field validation example:
            if (("POST".equals(method) || "PUT".equals(method)) && body == null) {
                throw new IllegalStateException(
                    method + " requests should include a body"
                );
            }
            return new HttpRequest(this);
        }
    }
}
```

### Director (optional): `CommonRequests`

A Director encapsulates common construction recipes so callers do not repeat boilerplate.

```java
package creational.builder.http;

/**
 * Director that knows how to assemble common request shapes.
 * Callers can use these directly or build requests manually.
 */
public final class CommonRequests {

    private CommonRequests() {} // utility class

    /** Build a standard JSON GET request with auth. */
    public static HttpRequest authenticatedJsonGet(String url, String bearerToken) {
        return new HttpRequest.Builder("GET", url)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .timeoutMillis(10_000)
                .build();
    }

    /** Build a JSON POST request with a body. */
    public static HttpRequest jsonPost(String url, String jsonBody) {
        return new HttpRequest.Builder("POST", url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(jsonBody)
                .build();
    }
}
```

### Client: `App`

```java
package creational.builder.http;

public class App {

    public static void main(String[] args) {

        // 1. Manual build with fluent API
        HttpRequest search = new HttpRequest.Builder("GET", "https://api.example.com/search")
                .header("Accept", "application/json")
                .queryParam("q", "builder pattern")
                .queryParam("page", "1")
                .timeoutMillis(5_000)
                .build();

        System.out.println("--- Manual GET ---");
        System.out.println(search);
        System.out.println();

        // 2. Using the Director for a common recipe
        HttpRequest authed = CommonRequests.authenticatedJsonGet(
                "https://api.example.com/me", "tok_abc123");

        System.out.println("--- Director GET (authenticated) ---");
        System.out.println(authed);
        System.out.println();

        // 3. POST with JSON body
        String payload = "{\"title\":\"Hello\",\"content\":\"World\"}";
        HttpRequest post = CommonRequests.jsonPost(
                "https://api.example.com/posts", payload);

        System.out.println("--- Director POST ---");
        System.out.println(post);
    }
}
```

### Expected Output

```
--- Manual GET ---
GET https://api.example.com/search?q=builder pattern&page=1
Accept: application/json
[timeout=5000ms]

--- Director GET (authenticated) ---
GET https://api.example.com/me
Accept: application/json
Authorization: Bearer tok_abc123
[timeout=10000ms]

--- Director POST ---
POST https://api.example.com/posts
Content-Type: application/json
Accept: application/json

{"title":"Hello","content":"World"}
[timeout=30000ms]
```

### Why This Works Well

- **Required fields** (`method`, `url`) are constructor parameters on the Builder -- you cannot forget them.
- **Optional fields** have sensible defaults and are set via named methods -- no positional ambiguity.
- **Fluent chaining** (`return this`) keeps the call site readable.
- **Validation** happens in `build()`, so the `HttpRequest` is never in an invalid state.
- **Immutability** -- the product's collections are wrapped with `Collections.unmodifiableMap()`, and all fields are `final`.
- **Director** (`CommonRequests`) captures reusable recipes without coupling them to the product's internals.

---

## 5. Tradeoffs and Limitations

### When to Use It

- The object has **four or more constructor parameters**, especially if several are optional or share the same type (making positional errors likely).
- You want the product to be **immutable** but construction requires multiple steps.
- You need to **validate cross-field invariants** before the object exists (e.g. "POST requests must have a body").
- You want a **fluent, self-documenting API** at the call site.
- You have **multiple representations** that share the same construction sequence (the GoF Director form).

### When Not to Use It

- The object has **few, obvious parameters** (two or three). A simple constructor is clearer and involves less ceremony.
- The object is **inherently mutable** and callers need to change fields after construction. A builder adds indirection without buying you immutability.
- You are in a context where **records** (Java 16+) or **data classes** solve the problem more concisely. A `record` with a compact canonical constructor may be all you need.

### Complexity Cost

- Every product class now has a companion builder class. In a large codebase with many small DTOs, this can produce significant boilerplate. Libraries like **Lombok** (`@Builder`) or **Immutables** can generate builders to reduce this cost.
- The builder duplicates every field of the product. If the product changes, the builder must change in lockstep.
- The Director role is often unnecessary in practice and can be omitted without loss.

### Alternatives to Consider

| Alternative | When to Prefer It |
|---|---|
| **Telescoping constructors** | Very few parameters, all required, distinct types. |
| **Static factory methods** | A small, fixed set of meaningful configurations (e.g. `HttpRequest.get(url)`). |
| **Java Records** | Simple immutable carriers with no complex validation or optional fields. |
| **Kotlin data classes / named arguments** | If your project uses Kotlin, named and default arguments eliminate most of the motivation for a builder. |
| **Prototype (clone)** | When you want to create variants of an existing object rather than building from scratch. |

### Relationship to Other Patterns

- **Abstract Factory** can use Builder internally to construct complex products.
- **Builder** and **Prototype** are both creational but solve different problems: Builder constructs step-by-step from nothing; Prototype copies an existing object.
- A **Fluent Interface** is a style choice often paired with Builder but is not the same pattern. You can have a builder without fluent chaining, and fluent chaining without a builder.
