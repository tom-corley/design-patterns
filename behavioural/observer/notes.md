# Observer

- This decouples the sending of an event, from the response to it, this pattern is near ubiquitous in event driven systems
- A object sends an event, and does not need to know who is listening, and how they respond, providing clean SoC
- Manages a one to many dependency cleanly
- Instead of having listeners depend on a subject to update them, invert the dependency and make it their responsibility to respond to the subjects changes, this leads to looser coupling

- Two ways for observers to recieve data
    - Push model: subject sends detailed change information in the update call, so that the observer recieves everything it needs without calling back to subject, this is simpler but leads to bloated push payloads to keep every observer happy (they may have different data needs)
    - Pull model: the subject recieves a minimal notification and then the observer calls back for whatever data it needs. This keeps notification slim but does reintroduce some coupling.

- In practice a hybrid approach is used.
- This pattern is the in-process ancestor of distributed pub/sub systems, Kafka topics and RabbitMQ exchanges just do this pattern at a larger scale, the difference being is that we have a bit more complexty to manage things across threads.
- One limitation is we call observers synchronously, we may need an async notifier dispatch at scale, or if there are ordering dependencies
- The big issue is the lapsed listener problem, if we stop using an observer object, but the pointer to it is still present on the subject, no matter what happens, the JVM cannot garbage collect it, so we are redundantly wasting memory and CPU cycles on that listener forever.
- If we are constantly adding and removing listeners, this piles up to performance issues quite quickly and can lead to memory leaks.
- To mitigate this, we should use explicit lifecyle management, so we need dispose() or onDestory() methods for listeners, or use weak references with hash maps, so garbage collection works.
- Another issue is concurrency, if an observer is added or removed while notify observers is iterating, this may lead to iteration issues, using a CopyOnWrite array helps here.
- There is also an issue with Reentrant notifications (runaway train) if an observers reaction in turn causes more notifications and so on. 
- By keeping notifications granular, every observer can filter for whats actually relevant etcetera. 