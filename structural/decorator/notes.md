# Decorator

- The basic problem the decorator pattern solves is a explosion of subclasses, if you add a new functionality option, with traditional inheritance, you have a multiplicative increase in the number of classes necessary to represent every possible configuration. The decorator solves this by just allowing us to wrap each configuration layer on top of the last.
- Allows dynamic behaviour at runtime
- Very Open/Closed, the base class is closed for modification, but open for extension because we can add new decorators to add behaviour.
- Tradeoff is that it is quite hard to debug, because each decorator by design has no idea how many decorators ar e under it and so the stack trace can be quite deep. It is also hard to tell which layer introduced which behaviour.
- Another potential issue is the application of the same decorator multiple times, though sometimes this is intentional. The workaround here is to use a fluent builder which tracks which decorators have been added, and encapsulate the wrapping logic.

## Pattern Structure

The idea is - There is a shared interace, say Coffee. that is implemented both by the base object, and the decorators (Say Vanilla Syrup), the key thing is this:

- BaseDecorator is an abstract class which implements the shared interface, and just passes all calls down to the wrapped concrete object
- The actual decorators are direct subclasses of the abstract base decorator
- The actual component (e.g the coffee) is a direct implementation of the component interface, not a subclass.

The Base decorator both implements the component interface, and stores a reference to the component it is wrapping (might be another decorator; or might be the actual object)

## Composition Chain

At runtime the decorators sort of form a linked list, each has a pointer to the next, the job of each method is to add any unique logic they want, then pass it on to the next.

