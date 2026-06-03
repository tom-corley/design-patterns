# Adapter

There is both a client and a lower level functionality that the client needs to use. If these two are incompatible, but cannot or should not be tightly coupled by changing either one, it is better to make an intermediate adapter layer.

Parts:
- Client depends on an interface (I in SOLID), the "Target" interface
- Adapter class implements the target interface and wraps the adaptee, translating the calls from the client to the lower level service, which is referenced in the class by composition
- Adaptee is the original lower level service.