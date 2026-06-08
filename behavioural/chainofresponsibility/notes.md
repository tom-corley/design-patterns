# Chain of Responsibility

- This pattern is used to give multiple handlers an attempt at dealing with a request. Handlers form a "Chain of responsibility" - in which each handler can either resolve the request directly, or forward it to the next handler in the chain
- This is great for security filters in Java, where later handlers should never even see badly authed or misconfigured requests or for middleware
- Can be implemented fairly simply by making each handler inherit from a shared abstract class which defines the process of dealing with a request, and then using a pointer to the next handler in the chain which can be null
- It is also helpful to throw if we try to forward the request at the end of the chain, indicating that none of the handlers in the chain were able to process the request.
- Do not use if you want every request to use a signle handler, also worth implementing good logging and tracing as at runtime it can be hard to understand which handler an error surfaced at, this is also where granular and thorough testing is important. 
- Ordering in a chain is also important, it should be enforced programmatically where possible, and thought through, for example with Spring Filter Chains, the most expensive handlers are put at the end so that bad requests can be rejected quickly without wasting CPU cycles on bad requests.