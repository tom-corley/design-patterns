# Facade

- Primary value of a facade is to provide a simplified interface to more complicated logic, or to put several complex subsystems behind one unified interface
- Very useful for a simplified interface to a repeatable multistep process - for example placing an order involves payments, dispatch, notifications etc, which are likely separate services.
- Facades should not fully encapsulate subsystems, they merely offer a simplified interface, if other code needs to use the subsystems directly, that should not be an issue.