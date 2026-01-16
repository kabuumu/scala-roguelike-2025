package game.save

import org.scalatest.funsuite.AnyFunSuite
import game.GameState
import map.{WorldMapGenerator, WorldMapConfig, WorldConfig, MapBounds, TileType}
import game.Point
import game.GameMode
import game.entity.Entity

class SaveSystemWorldMapTest extends AnyFunSuite {

  test("Save system must regenerate world map from seed to preserve terrain") {
    // 1. Generate a real world map using the generator
    val seed = 12345L
    val worldConfig = WorldMapConfig(
      worldConfig = WorldConfig(
        bounds = MapBounds(-20, 20, -20, 20),
        seed = seed,
        // Ensure some features exist
        grassDensity = 0.5,
        treeDensity = 0.1
      )
    )
    val originalWorldMap = WorldMapGenerator.generateWorldMap(worldConfig)

    // Pick a point that is NOT a dungeon tile but IS a terrain tile (e.g., Grass or Tree)
    // We want to verify this tile exists after load
    val terrainPoint = originalWorldMap.tiles
      .find { case (p, t) =>
        t == TileType.Grass1 || t == TileType.Tree
      }
      .map(_._1)
      .getOrElse(Point(5, 5))

    val originalTileType = originalWorldMap.tiles(terrainPoint)

    val originalState = GameState(
      playerEntityId = "player",
      entities = Vector(Entity(id = "player", components = Map.empty)),
      worldMap = originalWorldMap,
      dungeonFloor = 0, // 0 = Overworld
      gameMode = GameMode.Adventure
    )

    // 2. Serialize
    val json = SaveGameJson.serialize(originalState)

    // 3. Deserialize
    val restoredState = SaveGameJson.deserialize(json)

    // 4. Verify
    // The restored world map should have the same tile at terrainPoint
    val restoredTile = restoredState.worldMap.tiles.get(terrainPoint)

    assert(
      restoredTile.isDefined,
      s"Terrain tile at $terrainPoint was lost on save/load"
    )
    assert(
      restoredTile.get == originalTileType,
      s"Terrain tile mismatch! Expected $originalTileType, got ${restoredTile.get}"
    )

    // Verify seed is preserved
    assert(
      restoredState.worldMap.seed == seed,
      s"World seed was not preserved! Expected $seed, got ${restoredState.worldMap.seed}"
    )
  }
}
