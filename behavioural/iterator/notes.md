# Iterator

- Classical pattern of iterating through some collection of objects in an ordered way
- This is deeply embedded into most languages, `for` `of` loops in Python, Java etc
- This seperates the logic of how an iterator traverses through a collection, from the collection itself
- It also helps implement custom ways of traversing a collection, there all an iterator interface needs is
    - hasNext() - is there another element in the collection
    - next() - get it
- Consists of a iterator interface, a concrete actual iterator, and an aggregate, the thing you iterate over, both interface and concrete
- they provide flat sequential views of complicated nested structures, 
- Potential issues with concurrent modification, if you modify the collection while iterating through it, you can cause errors, skip positions etc
- You can fail-fast, snapshot or copy on iterate
- or copy on write, or use concurrent collections
- consider using Java streams, they have some useful features