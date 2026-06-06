# State

- The state pattern is used to model an object whose behaviour varies over time, and/or transitions betweeen states
- Often, we naively write code with branching logic that checks state in every method, this means that code has to branch over every state, leading to very brittle code and heavy refactors when new states are introduced. 
- By extracting each state to its own class, the context object can just delegate operations to whatever state object it currently holds, and adding a new state is just a new class implementing that interface (good for O/C) 
- A key factor is how we transition between states - there are two common approaches. The first is to let the states themselves decide which state to transition to, but this means states must be aware of eachother (can limit SoC). Alternatively, the context is in charge of state transitions, this centralises things but may re-introduce conditional logic in the context. In practice, state-driven transitions are usually more natural.
- Only use this if complexity of states necessitate it, and state transitions are clean, and make sure state is stored, not derived on the fly, can lead to performance overhead. 
- You are trading conditional complexity for class complexity, this is not always better, think about what is more managable
- There is a coupling trade-off on who owns state transitions
- Differs from strategies because objects move through a lifecycle of different states, are likely aware about other states, and swap more often, and trigger the state transitions themselves.