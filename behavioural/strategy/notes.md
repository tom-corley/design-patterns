# Strategy 

- Core idea is to define a family of algorithms for the same piece of functionality
- In a lot of cases, for things like payment logic where there is a huge amount of variation in the logic based on things like account type, it provides a cleaner SoC to write the algorithms separately but implementing the same 
- the main issue this avoids is complex branching logic in a shared algorithm, this violates the Open/Closed principle - implementing a new strategy type means that the shared code is editied so all existing code needs to be retested
- The context does not have to worry about the concrete logic which implements the algorithm at all, they are loosely coupled, and we can easily swap out strategies at runtime if necessary.
- Drawback is, if the algorithm itself depends on context internals, it may be worth considering whether the algorithm should actually be within the context.
- Client can choose strategy, or a factory assigns the strategies - this decouples client from concrete classes but does also mean there is a mapping layer, another apporach is to use dependency injection with an app container.