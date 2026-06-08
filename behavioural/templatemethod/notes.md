# Template Method

The Template Method pattern is used to define the template for a complex method that has multiple interchangable steps
- The canonical example is a payment might need to validate, debit the sender, then credit the recipient in that order, but the exact details of each of these steps can vary between subclassses
- To enforce the template, we need to use an abstract class 'final' keyword, we can do it with an interface using the 'default' keyword, but this deos not enforce the method as non-overridable which needs to be the goal
- However this means that we take up our one inheritance slot, so the best way to do it is with Composition over Inheritance, by having a processor in the object
- The pattern relies on the inversion of control principle, the abstract base class defining the template knows the concrete ordering of steps, the concrete subclasses do not need to know how the steps are orchestrated, but just the minutae of how each of them work.
- If there are optional steps, it is useful to define default implementations for them in the abstract base classes which are no-ops
- This pattern is used a lot in testing frameworks, we can configure the hooks like beforeEach() and afterEach(), but the framework itself (the abstraction) orchestrates the calling of the hooks and the full testing pipeline.
- The biggest drawback of this pattern is its reliance on inheritance, because we have the signle inheritance constraint, a fragile base class, and we can have deep hierachies which are difficult to reason about. 
- Differs from strategy in a few ways:
    - Strategy switches out the entire algorithm, template method allows different classes to implement the steps differently, but keeps a shared order of operations
    - Template methods are fixed at compile time, strategies can be switched out at runtime
    - Strategy uses composition, Template method uses inheritance, but can use a combination of both inheritance and composition
- Worth being careful to keep number of abstract methods small, document and enforce the call order, and maybe combine with stategy for complicated problems.

