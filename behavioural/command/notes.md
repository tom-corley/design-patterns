# Command

- The key strength of this pattern is it's ability to separate the what the do, from the when and how to do it
- It also means that someone invoking a command does not need to have any knowledge of the actual low level logic which implements it - the canonical example being a light switch being flipped, as a human (invoker) we have no understanding of how that actually works
- It also encapsulates the action well in a way that allows us to reverse it or undo it
- This pattern allows us to store and move around actions and schedule them, which is a powerful tool.