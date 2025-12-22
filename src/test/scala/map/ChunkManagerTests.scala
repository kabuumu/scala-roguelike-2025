package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class ChunkManagerTests extends AnyFunSuite {

  test("Chunk.toChunkCoords calculates correct chunk coordinates") {
    val size = Chunk.size

    assert(Chunk.toChunkCoords(Point(0, 0)) == (0, 0))
    assert(Chunk.toChunkCoords(Point(size - 1, size - 1)) == (0, 0))
    assert(Chunk.toChunkCoords(Point(size, 0)) == (1, 0))
    assert(Chunk.toChunkCoords(Point(-1, 0)) == (-1, 0))
    assert(Chunk.toChunkCoords(Point(-size, 0)) == (-1, 0))
    assert(Chunk.toChunkCoords(Point(-size - 1, 0)) == (-2, 0))
  }

  test("ChunkManager generates chunks around player") {
    val seed = 12345L
    val bounds = MapBounds(0, 100, 0, 100)
    val config = WorldConfig(bounds, seed = seed)
    val worldConfig = WorldMapConfig(config)

    // Create initial world map
    val worldMap = WorldMapGenerator.generateWorldMap(worldConfig)

    // Initial map typically has no chunks or minimal
    // But we are testing dynamic generation

    val playerPos = Point(0, 0)

    val updatedMap =
      ChunkManager.updateChunks(playerPos, worldMap, config, seed)

    // Should have chunks loaded around (0,0) with radius 5
    // Range -5 to +5 in both X and Y = 11x11 = 121 chunks
    assert(updatedMap.chunks.nonEmpty, "Chunks should be generated")
    assert(updatedMap.chunks.contains((0, 0)), "Center chunk should be present")

    val chunkCount = updatedMap.chunks.size
    assert(
      chunkCount == 121,
      s"Should generate 121 chunks for radius 5, got $chunkCount"
    )
  }

  test("ChunkManager updates tiles in WorldMap") {
    val seed = 12345L
    val bounds = MapBounds(0, 100, 0, 100)
    val config = WorldConfig(bounds, seed = seed)
    val worldConfig = WorldMapConfig(config)

    val worldMap = WorldMapGenerator.generateWorldMap(worldConfig)
    val initialTileCount = worldMap.tiles.size

    val playerPos = Point(1000, 1000) // Far away to ensure new chunks
    val updatedMap =
      ChunkManager.updateChunks(playerPos, worldMap, config, seed)

    assert(
      updatedMap.tiles.size > initialTileCount,
      "Tiles should be added to WorldMap"
    )

    // Check if new tiles are in the chunks
    val chunkCoords = Chunk.toChunkCoords(playerPos)
    assert(
      updatedMap.chunks.contains(chunkCoords),
      "Chunk at player pos should be generated"
    )

    val chunk = updatedMap.chunks(chunkCoords)
    assert(chunk.tiles.nonEmpty, "Chunk should have tiles")

    // Check if chunk tiles are merged into worldMap.tiles
    val someTilePoint = chunk.tiles.keys.head
    assert(
      updatedMap.tiles.contains(someTilePoint),
      "WorldMap tiles should contain chunk tiles"
    )
  }

  test("ChunkManager is deterministic via seed") {
    val seed = 999L
    val bounds = MapBounds(0, 100, 0, 100)
    val config = WorldConfig(bounds, seed = seed)
    val worldConfig = WorldMapConfig(config)
    val worldMap = WorldMapGenerator.generateWorldMap(worldConfig)

    val playerPos = Point(500, 500)

    val map1 = ChunkManager.updateChunks(playerPos, worldMap, config, seed)
    val map2 = ChunkManager.updateChunks(playerPos, worldMap, config, seed)

    assert(map1.chunks.keys == map2.chunks.keys)
    // Check content of a chunk
    val coords = Chunk.toChunkCoords(playerPos)
    assert(map1.chunks(coords).tiles == map2.chunks(coords).tiles)
  }

  test("ChunkManager generates water tiles") {
    // With Perlin noise, we can't guarantee water in a small area without specific seeds,
    // but with enough area or specific config we should see some.
    // Our logic generates water for lakes (high moisture) and rivers (ridged noise).

    val seed = 12345L
    val bounds = MapBounds(0, 50, 0, 50)
    val config = WorldConfig(bounds, seed = seed)
    val worldConfig = WorldMapConfig(config)
    val worldMap = WorldMapGenerator.generateWorldMap(worldConfig)

    // Check a larger area to ensure we hit some water features
    // Iterate until we find water or give up
    var foundWater = false
    var chunkX = 0

    // Scan a few chunks
    while (!foundWater && chunkX < 10) {
      val playerPos = Point(chunkX * Chunk.size, 0)
      val updatedMap =
        ChunkManager.updateChunks(playerPos, worldMap, config, seed)

      foundWater = updatedMap.tiles.values.exists(_ == TileType.Water)
      chunkX += 1
    }

    assert(
      foundWater,
      "Should generate some water tiles across multiple chunks"
    )
  }
}
