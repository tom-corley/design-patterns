# Mediator

- This pattern is used to simplify communication between a group of objects, by routing all of their communication between a middleman - a mediator
- This also means the group are loosely coupled as they do not need to hold direct references to eachother, but instead just a reference to the mediator, so instead of a messy grid topology, it is a star.
- This is better for Open/Closed, as now you can add new dependents without having to go back and change the old objects to couple them to the new ones
- You are trading many small couplings for one very large one, so one risk is the mediator becoming overly complex, somewhat of a god object potentially. Some fixes:
    - Decompose the mediator into sub mediators
    - Use event based notification within the mediator to keep internal logic delarative
    - Keep business logic out as much as possible, should just be routing

Structure:
- Mediator interface, a method like notify(sender, event)
- ConcreteMediator, implements the coordination logic, wholes references to all concrete colleagues and reacts to their notification by invoking methods on other colleagues
- Colleague, interface for an object that is mediated, each colleague must hold a reference to its mediator
- ConcreteColleague is just an object implementing colleague

Mediator vs Observer
- Observer is one-to-many communication, one object publishes, a list of observers/subscribers get notified, where as mediator handles many to many communication, where multiple colleagues both notify and get notified
- The intelligence is distributed in the observer pattern, the observers own their reaction to the notification, where as in the mediator this logic is cnetralised
- The coupling is different, as observers are independent of eachother and coupled to the event interface, where as in the mediator pattern, the mediator is coupled to all colleagues.