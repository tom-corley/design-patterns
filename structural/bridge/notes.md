# Bridge

- Sometimes a program requires a complicated service or object that has multiple dimensions of variation that are completely independent (orthogonal) to each other. 
- By letting these two dimensions vary independently (e.g) in different class hierachies we are able to avoid the subclass explosion issue that would come with an inheritance approach which simply creates a class for every possible configuration.
- A Bridge is an object which points to a subclass in each hierachy, and encapsulates the whole thing
