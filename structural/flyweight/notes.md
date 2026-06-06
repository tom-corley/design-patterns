# Flyweight

The core idea is to save memory to ensure that repeated objects are not repeated in full - wasting huge amounts of memory. The canonical example is character glyphs on e.g microsoft word, would be foolish to store a full graphics object per letter when repeated constantly.

- Intrinsic state is stuff that is common to every instance of an object of a certain type, e.g graphics information, this should be immutable and arguably thread safe as it is shared around.
- Extrinsic state is stuff that is specific to a given instance, e.g position or color
- The idea is to use a factory to ensure there is only ever one instance of a given intrinsic state (A flyweight) other objects just wrap that shared intrinsic state with a pointer