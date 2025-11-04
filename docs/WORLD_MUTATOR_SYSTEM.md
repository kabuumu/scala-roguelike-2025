# World Mutator System

## Overview

The World Mutator System is an extensible architecture for world generation that allows composable, modular world building. Similar to the existing `DungeonMutator` system, it breaks down world generation into discrete, reusable steps.

## Architecture

### Core Trait

```scala
trait WorldMutator {
  def mutateWorld(worldMap: WorldMap): WorldMap
}
```

Every mutator implements this simple interface, taking a `WorldMap` as input and returning a transformed `WorldMap`.

### Built-in Mutators

#### 1. TerrainMutator
Generates base terrain (grass, dirt, trees) using noise generation.

```scala
val terrainMutator = new TerrainMutator(worldConfig)
```

**Purpose**: Creates the foundation terrain layer with natural-looking patterns.

#### 2. DungeonPlacementMutator
Places dungeons in the world using provided dungeon configurations.

```scala
val dungeonMutator = new DungeonPlacementMutator(dungeonConfigs)
```

**Purpose**: Adds dungeon structures to the world, including rooms, corridors, and dungeon-specific tiles.

#### 3. ShopPlacementMutator
Places a shop building in the world near the spawn point.

```scala
val shopMutator = new ShopPlacementMutator(worldBounds)
```

**Purpose**: Adds a shop structure where players can buy and sell items.

#### 4. PathGenerationMutator
Creates dirt paths from a starting point to all destinations (dungeons, shops).

```scala
val pathMutator = new PathGenerationMutator(startPoint)
```

**Purpose**: Ensures there are clear paths between important locations using Bresenham's line algorithm.

#### 5. WalkablePathsMutator
Optional mutator that ensures walkable connectivity by clearing obstacles.

```scala
val walkableMutator = new WalkablePathsMutator(worldConfig)
```

**Purpose**: Post-processes the world to ensure players can navigate between areas.

## Usage

### Standard Generation

The simplest way to use the mutator system is through `WorldMapGenerator`:

```scala
val config = WorldMapConfig(
  worldConfig = WorldConfig(bounds, seed = 12345)
)
val worldMap = WorldMapGenerator.generateWorldMap(config)
```

This applies the standard mutator sequence:
1. Terrain generation
2. Dungeon placement
3. Shop placement
4. Path generation
5. Walkable paths (if enabled)

### Custom Mutator Lists

For custom world generation, you can specify your own mutator sequence:

```scala
val mutators = Seq(
  new TerrainMutator(worldConfig),
  new CustomMutator(),  // Your custom mutator
  new PathGenerationMutator(startPoint)
)

val initialWorld = WorldMap(/* ... */)
val finalWorld = WorldMapGenerator.generateWorldMapWithMutators(initialWorld, mutators)
```

## Creating Custom Mutators

To add new world generation features, create a class that implements `WorldMutator`:

```scala
class PerimeterRockMutator(bounds: MapBounds) extends WorldMutator {
  override def mutateWorld(worldMap: WorldMap): WorldMap = {
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    
    val perimeterRocks = (for {
      x <- tileMinX to tileMaxX
      y <- tileMinY to tileMaxY
      if x == tileMinX || x == tileMaxX || y == tileMinY || y == tileMaxY
    } yield Point(x, y) -> TileType.Rock).toMap
    
    worldMap.copy(tiles = worldMap.tiles ++ perimeterRocks)
  }
}
```

### Best Practices for Custom Mutators

1. **Single Responsibility**: Each mutator should do one thing well
2. **Immutability**: Always return a new `WorldMap` rather than modifying the input
3. **Composition**: Design mutators to work well with others in sequence
4. **Configuration**: Accept configuration parameters in the constructor
5. **Idempotency**: When possible, design mutators to be safely re-applied

## Example: River Generation

Here's how you might add a river generation mutator:

```scala
class RiverGenerationMutator(riverConfigs: Seq[RiverConfig]) extends WorldMutator {
  override def mutateWorld(worldMap: WorldMap): WorldMap = {
    val rivers = riverConfigs.flatMap { config =>
      RiverGenerator.generateRiver(config)
    }
    
    val riverTiles = rivers.flatMap(_.tiles).toMap
    
    worldMap.copy(
      tiles = worldMap.tiles ++ riverTiles,
      rivers = worldMap.rivers ++ rivers.flatMap(_.points)
    )
  }
}
```

Then use it in your generation sequence:

```scala
val mutators = Seq(
  new TerrainMutator(worldConfig),
  new RiverGenerationMutator(riverConfigs),  // Add rivers
  new DungeonPlacementMutator(dungeonConfigs),
  new PathGenerationMutator(startPoint)
)
```

## Advantages

1. **Extensibility**: Easy to add new world features without modifying existing code
2. **Testability**: Each mutator can be tested independently
3. **Reusability**: Mutators can be reused in different generation scenarios
4. **Composability**: Mix and match mutators to create different world types
5. **Maintainability**: Clear separation of concerns for each generation step

## Testing

The test suite includes comprehensive tests for:
- Individual mutator functionality
- Mutator chaining
- Custom mutator creation
- Integration with `WorldMapGenerator`

See `WorldMutatorTest.scala` for examples.

## Comparison with DungeonMutator

| Aspect | DungeonMutator | WorldMutator |
|--------|----------------|--------------|
| Purpose | Generate dungeon rooms and features | Generate world terrain and structures |
| Input/Output | `Dungeon` → `Set[Dungeon]` (options) | `WorldMap` → `WorldMap` (single result) |
| Application | Explores possibilities, returns options | Directly applies transformation |
| Use Case | Constraint-based generation | Sequential composition |

The key difference is that `DungeonMutator` returns a set of possible mutations (for exploring the solution space), while `WorldMutator` directly applies a transformation (for sequential composition).

## Future Enhancements

Potential additions to the mutator system:

1. **BiomeGenerationMutator**: Create different biomes (desert, forest, snow)
2. **VillageGenerationMutator**: Place villages with NPCs
3. **RoadNetworkMutator**: Create interconnected road systems
4. **LakeGenerationMutator**: Add bodies of water
5. **BridgeGenerationMutator**: Build bridges over rivers
6. **FortificationMutator**: Add walls and defensive structures

Each of these can be implemented as a `WorldMutator` and integrated into the generation flow.
