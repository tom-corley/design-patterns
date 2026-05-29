# Flyweight Pattern

**Classification:** Structural (GoF)

---

## 1. Core Idea

The Flyweight pattern reduces memory consumption by sharing as much state as possible between similar objects. Instead of each object storing all of its own data independently, the pattern separates object state into two categories:

- **Intrinsic state** -- data that is invariant, context-free, and shareable. It lives inside the flyweight object and is set once at creation time. Multiple clients reference the same flyweight instance without conflict because this state never changes.

- **Extrinsic state** -- data that varies per usage context. It is *not* stored inside the flyweight. Instead, the client computes or looks it up and passes it to the flyweight at call time.

### The Problem

Consider a text editor that renders a document containing 100,000 characters. A naive implementation might create 100,000 independent objects, each storing the character's glyph image, font metrics, Unicode codepoint, colour, and screen position. Most of those characters are repeats -- there are only ~80 distinct printable ASCII characters. Storing a unique glyph object per character wastes enormous amounts of memory.

The Flyweight pattern solves this by creating only one glyph object per distinct character (the intrinsic state: glyph image, font metrics, codepoint). The position and colour where each character appears on screen (extrinsic state) is stored externally and passed in when the glyph needs to render itself.

### The Key Insight

The pattern works whenever a large number of objects can be replaced by a small number of shared objects once the context-dependent state is extracted.

---

## 2. Structure

### Participants

| Participant | Role |
|---|---|
| **Flyweight** | Declares the interface through which flyweights receive and act on extrinsic state. |
| **ConcreteFlyweight** | Implements the Flyweight interface and stores intrinsic state. Must be shareable. Any state it stores must be intrinsic -- independent of the flyweight's context. |
| **FlyweightFactory** | Creates and manages flyweight objects. Ensures flyweights are shared properly: when a client requests a flyweight, the factory returns an existing instance or creates a new one if none exists. |
| **Client** | Maintains a reference to flyweight(s) and computes or stores the extrinsic state that flyweights need at operation time. |

### Relationships

```
Client
  |
  |-- stores extrinsic state (position, colour, etc.)
  |-- requests flyweights from FlyweightFactory
  |
  v
FlyweightFactory
  |
  |-- maintains a pool (Map) of shared flyweight instances
  |-- returns existing instance or creates new one
  |
  v
ConcreteFlyweight (implements Flyweight)
  |
  |-- stores intrinsic state only (glyph data, texture, etc.)
  |-- receives extrinsic state as method parameters
```

The factory is the gatekeeper. Clients never instantiate flyweights directly -- they always go through the factory, which is what enables sharing.

---

## 3. Use Cases

### Text Rendering Engines

Each distinct character glyph is a flyweight. Intrinsic state: the glyph bitmap, font metrics, Unicode codepoint. Extrinsic state: x/y position on the page, colour, font size applied at render time. A 500-page document reuses the same ~100 glyph flyweights millions of times.

### Game Tile Maps

A tile-based game map might have millions of tiles but only a few dozen tile types (grass, water, stone, sand). Each tile type is a flyweight holding the texture, passability flag, and rendering rules. The grid coordinates and per-tile damage state are extrinsic.

### Connection / Thread Pools

Database connection pools are a form of flyweight. The connection object (with its socket, protocol negotiation state, etc.) is the shared resource. The specific query being executed is extrinsic state passed in by the client.

### GUI Frameworks

Borders, icons, and style definitions in UI toolkits are often flyweights. The same "raised bevel border" object can be shared across hundreds of buttons rather than creating a new border instance per component.

### String Interning

`String.intern()` in Java is essentially a flyweight mechanism. The JVM maintains a pool of string literals and returns the canonical instance for any given character sequence, avoiding duplicate allocations.

---

## 4. Example in Java

This example models a **map tile rendering system** for a 2D game. Tile types (grass, water, stone, sand) are flyweights. Their textures and traversal costs are intrinsic. Their grid position and any per-cell damage level are extrinsic.

### MapTile.java -- the Flyweight interface

```java
/**
 * Flyweight interface. Defines the operation that accepts extrinsic state.
 */
public interface MapTile {

    /** Render this tile at the given grid position with the specified damage overlay. */
    void render(int gridX, int gridY, int damageLevel);

    String getTerrain();
}
```

### ConcreteMapTile.java -- ConcreteFlyweight

```java
/**
 * ConcreteFlyweight. Stores intrinsic state only: the terrain type,
 * the texture file path, and the base traversal cost.
 * Instances of this class are shared across many grid cells.
 */
public class ConcreteMapTile implements MapTile {

    // --- Intrinsic state (shared, immutable) ---
    private final String terrain;
    private final String textureFilePath;
    private final double traversalCost;

    ConcreteMapTile(String terrain, String textureFilePath, double traversalCost) {
        this.terrain = terrain;
        this.textureFilePath = textureFilePath;
        this.traversalCost = traversalCost;
        // Simulate expensive texture loading
        System.out.println("  [Loading texture for '" + terrain + "' from " + textureFilePath + "]");
    }

    /**
     * Extrinsic state (gridX, gridY, damageLevel) is passed in -- not stored.
     */
    @Override
    public void render(int gridX, int gridY, int damageLevel) {
        System.out.printf("    Rendering '%s' tile at (%d, %d) | traversal=%.1f | damage=%d%n",
                terrain, gridX, gridY, traversalCost, damageLevel);
    }

    @Override
    public String getTerrain() {
        return terrain;
    }
}
```

### TileFactory.java -- FlyweightFactory

```java
import java.util.HashMap;
import java.util.Map;

/**
 * FlyweightFactory. Ensures that each terrain type is instantiated at most once.
 * Subsequent requests for the same terrain return the cached instance.
 */
public class TileFactory {

    private final Map<String, MapTile> tilePool = new HashMap<>();

    /**
     * Return the shared flyweight for the given terrain type.
     * If it does not yet exist, create it and cache it.
     */
    public MapTile getTile(String terrain) {
        return tilePool.computeIfAbsent(terrain, key -> {
            switch (key) {
                case "grass":
                    return new ConcreteMapTile("grass", "/textures/grass.png", 1.0);
                case "water":
                    return new ConcreteMapTile("water", "/textures/water.png", 3.0);
                case "stone":
                    return new ConcreteMapTile("stone", "/textures/stone.png", 1.5);
                case "sand":
                    return new ConcreteMapTile("sand", "/textures/sand.png", 2.0);
                default:
                    throw new IllegalArgumentException("Unknown terrain: " + key);
            }
        });
    }

    /** Report how many unique flyweight instances exist. */
    public int poolSize() {
        return tilePool.size();
    }
}
```

### GameMap.java -- Client

```java
import java.util.Random;

/**
 * Client. Stores extrinsic state (grid position, per-cell damage)
 * and references shared flyweight tile objects.
 */
public class GameMap {

    private final int width;
    private final int height;
    private final MapTile[][] tiles;     // references to shared flyweights
    private final int[][] damageLevels;  // extrinsic state per cell

    public GameMap(int width, int height, TileFactory factory) {
        this.width = width;
        this.height = height;
        this.tiles = new MapTile[height][width];
        this.damageLevels = new int[height][width];

        String[] terrains = {"grass", "water", "stone", "sand"};
        Random rng = new Random(42);

        System.out.println("Building " + width + "x" + height + " map (" + (width * height) + " cells)...");

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Each cell gets a shared flyweight -- NOT a new object
                tiles[y][x] = factory.getTile(terrains[rng.nextInt(terrains.length)]);
                damageLevels[y][x] = rng.nextInt(4); // 0-3 damage
            }
        }

        System.out.println("Map built. Unique tile objects in pool: " + factory.poolSize());
    }

    /** Render a subsection of the map. */
    public void renderRegion(int startX, int startY, int regionWidth, int regionHeight) {
        System.out.printf("%nRendering region (%d,%d) to (%d,%d):%n",
                startX, startY,
                startX + regionWidth - 1, startY + regionHeight - 1);

        for (int y = startY; y < startY + regionHeight && y < height; y++) {
            for (int x = startX; x < startX + regionWidth && x < width; x++) {
                // Extrinsic state (x, y, damage) is passed to the flyweight at render time
                tiles[y][x].render(x, y, damageLevels[y][x]);
            }
        }
    }

    public static void main(String[] args) {
        TileFactory factory = new TileFactory();

        // Create a large map -- only 4 tile objects are ever created
        GameMap map = new GameMap(1000, 1000, factory);

        // Render a small visible region
        map.renderRegion(0, 0, 3, 3);

        System.out.println("\nTotal grid cells: " + 1000 * 1000);
        System.out.println("Unique flyweight tile objects: " + factory.poolSize());
        System.out.println("Memory saved: ~"
                + (1000 * 1000 - factory.poolSize()) + " redundant objects avoided");
    }
}
```

### Expected Output

```
Building 1000x1000 map (1000000 cells)...
  [Loading texture for 'sand' from /textures/sand.png]
  [Loading texture for 'grass' from /textures/grass.png]
  [Loading texture for 'water' from /textures/water.png]
  [Loading texture for 'stone' from /textures/stone.png]
Map built. Unique tile objects in pool: 4

Rendering region (0,0) to (2,2):
    Rendering 'sand' tile at (0, 0) | traversal=2.0 | damage=0
    Rendering 'grass' tile at (1, 0) | traversal=1.0 | damage=3
    Rendering 'water' tile at (2, 0) | traversal=3.0 | damage=0
    Rendering 'sand' tile at (0, 1) | traversal=2.0 | damage=1
    Rendering 'stone' tile at (1, 1) | traversal=1.5 | damage=2
    Rendering 'grass' tile at (2, 1) | traversal=1.0 | damage=3
    Rendering 'water' tile at (0, 2) | traversal=3.0 | damage=0
    Rendering 'sand' tile at (1, 2) | traversal=2.0 | damage=2
    Rendering 'grass' tile at (2, 2) | traversal=1.0 | damage=1

Total grid cells: 1000000
Unique flyweight tile objects: 4
Memory saved: ~999996 redundant objects avoided
```

One million grid cells, four objects. That is the Flyweight pattern in action.

---

## 5. Tradeoffs and Limitations

### When to Use It

- You have a very large number of objects that are expensive to create or store.
- Most of the object state can be made extrinsic (moved outside the object).
- The number of *distinct* shared states is small relative to the total number of usages.
- The application does not depend on object identity -- clients should not rely on `==` comparisons for distinct logical entities that happen to share the same flyweight.

### When NOT to Use It

- The number of distinct objects is already small. Flyweight adds factory and state-separation complexity for no benefit if you only have a few dozen instances.
- Most state is context-dependent. If there is very little intrinsic state to share, the flyweight objects are nearly empty shells, and the extrinsic state management costs more than it saves.
- Object identity matters to your domain logic. Since flyweights are shared, two logically distinct entities may reference the same object instance. This breaks code that uses reference equality.

### Threading Concerns

Flyweight objects must be **immutable** (or at least effectively immutable after construction) to be safely shared across threads. If a flyweight's intrinsic state were mutable, concurrent modifications would corrupt all clients sharing that instance.

The **FlyweightFactory** needs synchronisation if it is accessed from multiple threads. Options include:

- `ConcurrentHashMap` with `computeIfAbsent` (preferred in most Java applications).
- A `synchronized` block around the pool lookup.
- Eagerly initialising all flyweights at startup so the pool is read-only at runtime, eliminating contention entirely.

### Complexity of Separating State

The hardest part of applying this pattern is cleanly dividing state into intrinsic and extrinsic. A wrong split leads to either:

- **Too much intrinsic state** -- the number of distinct flyweights explodes, defeating the purpose.
- **Too much extrinsic state** -- clients become burdened with managing and passing large amounts of context on every operation, which can hurt readability and performance (cache misses, parameter passing overhead).

Getting the split right requires understanding which dimensions of your data actually vary per-usage-site versus which are shared across many sites.

### Relationship to Other Patterns

- **Flyweight + Factory Method/Abstract Factory** -- the FlyweightFactory is itself a factory pattern, often implemented with a `Map`-based cache.
- **Flyweight + Composite** -- flyweights are frequently leaf nodes in composite trees (e.g. characters in a document tree). The composite provides the structure; the flyweight provides memory-efficient leaves.
- **Flyweight vs Singleton** -- a Singleton ensures one instance of a class globally. A Flyweight Factory manages one instance *per distinct intrinsic state*. There may be many flyweight instances, but far fewer than the number of usages.
- **Flyweight vs Object Pool** -- an object pool recycles mutable objects to avoid allocation cost. A flyweight shares immutable objects to avoid duplication. Pools lend and reclaim; flyweights share permanently.
