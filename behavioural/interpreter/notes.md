# Interpreter

This is a method for resolving a language once you have already broken it down into an AST (Abstract Syntax Tree) - one of the tougher patterns to get. 
- The basics of how it works:
    - We parse a sentence in a language into an AST, and we call interpret on the root of the AST.
    - interpret() comes from an AbstractExpression interface, which is implemented by two subclasses
        - TerminalExpression for terminal symbols in the grammar which do not depend on other nodes to evaluate
        - NonTerminalExpression, this will basically do some logic that includes, using interpret on the children in some way
    - There is also a context class which might store things like variable bindings or maps
- The construction of an AST for a sentence is completely out of scope for this pattern, though a parser is usually needed for that part.
- Good for evaluating mathematical expressions, or SQL parsing, and regular expressions.
- One of the advantages of this pattern is its extendability
- And its reusability
- But can lead to a class explosion issue, and is slow as it is a recursive tree walk, so optimisation necessary for certain cases