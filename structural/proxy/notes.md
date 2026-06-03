# Proxy

- Client needs to do something, but we want a control layer between the client and the actual logic, or we want to do something like lazy loading.
- We define a shared interface implemented by both the proxy and the concrete logic
- The proxy stores a pointer to the concrete implemetation, and uses it after some additional validation etc. 
- Good for objects that are expensive to create, we do not actually create it until it is needed at runtime
- We can compose multiple proxies. e.g a caching proxy aswell as a access control one,. It is good for managing multiple cross cutting concerns.
- Avoid using for when the interface to use the service needs to change.