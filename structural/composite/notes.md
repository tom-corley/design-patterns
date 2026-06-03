# Composite

- Seems to just be a tree structure where an operation recursively cascades through subcomponents
- The core idea is that the client codes to the interface, and then the composites or leaves follow the same contract, ensuring the client does not need to know or care about the hierachical structure of data at runtime.
- Debate over whether add and remove should be in the interface (which means leaves must implement them just for them to throw), or to implement them just on the composite, but this means client needs to know a bit.