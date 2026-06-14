# Visitor

- The main idea of this patttern is to get around third party or legacy classes that cannot be edited directly, you "visit" the class
- Single dispatch, the idea that the method called depends on the runtime type of the reciever (for example overriding a method call via a subclass or an implementation of an interface)
- Double dispatch, which is not natively supported by most languges
- The key idea here, is that to add a new behaviour to a class hierachy or interface, you have to add an implementation for every subclass, so operations are expensive
- Opposingly, adding a new type is easy, all it has to do is fulfill the interface's contract and everything just works
- Visitor inverts this rule, it means that operations are very easy to add, you just add one visitor class, but adding a new type means adjusting every single visitor to work for that type to
- Therefore, you use visitors only when there are lots of new operations but the types are pretty stable.
- The point of the double dispatch thing, is that a method called with an argument that is an interface does not have to rely on fragile instanceof checks
- First we dispatch which subtype it is, and then we dispatch which method we call, based on the subtype
- Sealed classes is a nicer way to do some of this stuff does, simplifies code and means that the compiler can deduce a method exists if it exists on all of the finite subclases. 