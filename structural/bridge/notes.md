# Bridge

- Sometimes a program requires a complicated service or object that has multiple dimensions of variation that are completely independent (orthogonal) to each other. 
- By letting these two dimensions vary independently (e.g) in different class hierachies we are able to avoid the subclass explosion issue that would come with an inheritance approach which simply creates a class for every possible configuration.
- A Bridge is an object in the abstraction which also holds a pointer to the implementation interface, it does not need to know how the implementation works at runtime, just that it implements the required contract.
- The drive behind this is preferring composition over inheritance, and decoupling abstraction and implementation
- One example is notifications, lets say there are both Alerts and Reminders, as types of notifications, but also there are both emails, and sms messages as delivery channels. We can bridge these together.


# Structure

There is both a base abstraction and a base implementation which need to be concretified:
- The base implementation only needs behaviour and not state, so is best represented with an interface
- Meanwhile the base abstraction needs state (it needs to hold a pointer to the implementation it uses), so should be an abstract base class and not an interface