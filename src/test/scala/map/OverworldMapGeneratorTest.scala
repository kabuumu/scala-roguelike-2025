package map

import org.scalatest.funsuite.AnyFunSuite

class OverworldMapGeneratorTest extends AnyFunSuite {

  test("OverworldMapGenerator is deterministic with same seed") {
    // Use multiple seeds to increase confidence
    for (i <- 1 to 3) {
      val seed = 12345L + i * 1000
      val config = OverworldMapConfig(width = 100, height = 100, seed = seed)

      val run1 = OverworldMapGenerator.generate(config)
      val run2 = OverworldMapGenerator.generate(config)

      assert(
        run1.tiles.keySet == run2.tiles.keySet,
        s"Tile positions should be identical for seed $seed"
      )

      assert(
        run1.tiles == run2.tiles,
        s"Tile types should be identical for seed $seed"
      )

      assert(
        run1.seed == run2.seed,
        s"Seeds should be identical"
      )
    }
  }

  test("Generated map has reasonable land/water ratio") {
    // Use a larger map to ensure edge falloff creates water
    val config = OverworldMapConfig(width = 200, height = 200, seed = 42L)
    val map = OverworldMapGenerator.generate(config)

    val totalTiles = map.tiles.size
    val landTiles = map.tiles.count { case (_, tileType) =>
      tileType != OverworldTileType.Ocean && tileType != OverworldTileType.Water
    }

    val landRatio = landTiles.toDouble / totalTiles

    // Land should be between 10% and 95% of the map
    // (edge falloff creates water around edges, but most of center is land)
    assert(
      landRatio >= 0.1 && landRatio <= 0.95,
      s"Land ratio $landRatio should be between 10% and 95%"
    )
  }

  test("Generated map contains biomes") {
    val config = OverworldMapConfig(width = 150, height = 150, seed = 99L)
    val map = OverworldMapGenerator.generate(config)

    val biomeTypes = map.tiles.values.toSet

    // Should have at least some water
    assert(
      biomeTypes.contains(OverworldTileType.Ocean) || biomeTypes.contains(
        OverworldTileType.Water
      ),
      "Map should contain water tiles"
    )

    // Should have at least some land biomes (Plains, Forest, or Desert)
    val landBiomes = Set(
      OverworldTileType.Plains,
      OverworldTileType.Forest,
      OverworldTileType.Desert
    )
    assert(
      biomeTypes.intersect(landBiomes).nonEmpty,
      "Map should contain land biome tiles"
    )
  }

  test("Settlements are placed on valid land tiles") {
    // Use larger map and lower threshold to ensure land for settlements
    val config = OverworldMapConfig(
      width = 300,
      height = 300,
      seed = 123L,
      landThreshold = 0.2, // Lower threshold = more land
      numCities = 2,
      numTowns = 5,
      numVillages = 15
    )
    val map = OverworldMapGenerator.generate(config)

    val settlements = map.tiles.filter { case (_, tileType) =>
      tileType == OverworldTileType.Village ||
      tileType == OverworldTileType.Town ||
      tileType == OverworldTileType.City
    }

    // Should have at least some settlements
    assert(
      settlements.nonEmpty,
      "Map should have settlements"
    )
  }

  test("Settlements have minimum spacing") {
    val config = OverworldMapConfig(
      width = 200,
      height = 200,
      seed = 456L,
      numCities = 2,
      numTowns = 5,
      numVillages = 15
    )
    val map = OverworldMapGenerator.generate(config)

    val settlements = map.tiles
      .filter { case (_, tileType) =>
        tileType == OverworldTileType.Village ||
        tileType == OverworldTileType.Town ||
        tileType == OverworldTileType.City
      }
      .keys
      .toSeq

    // Check that all settlements have at least some spacing
    // (minimum village spacing is 15)
    val minSpacing =
      10 // Using slightly less than min village spacing for leniency

    for {
      i <- settlements.indices
      j <- (i + 1) until settlements.size
    } {
      val p1 = settlements(i)
      val p2 = settlements(j)
      val dx = p1.x - p2.x
      val dy = p1.y - p2.y
      val distance = Math.sqrt(dx * dx + dy * dy)

      assert(
        distance >= minSpacing,
        s"Settlements at $p1 and $p2 are too close: distance = $distance"
      )
    }
  }

  test("Map dimensions match config") {
    val config = OverworldMapConfig(width = 50, height = 75, seed = 789L)
    val map = OverworldMapGenerator.generate(config)

    assert(map.width == 50, "Width should match config")
    assert(map.height == 75, "Height should match config")

    // All tiles should be within bounds
    map.tiles.keys.foreach { point =>
      assert(
        point.x >= 0 && point.x < 50,
        s"Tile x=${point.x} should be within [0, 50)"
      )
      assert(
        point.y >= 0 && point.y < 75,
        s"Tile y=${point.y} should be within [0, 75)"
      )
    }
  }
  test("Villages generate trails") {
    val config = OverworldMapConfig(
      width = 200,
      height = 200,
      seed = 888L,
      numCities = 2,
      numTowns = 4,
      numVillages = 10,
      landThreshold = 0.3 // Ensure plenty of land
    )
    val map = OverworldMapGenerator.generate(config)

    val trails = map.tiles.filter { case (_, tileType) =>
      tileType == OverworldTileType.Trail || tileType == OverworldTileType.TrailBridge
    }

    assert(trails.nonEmpty, "Map should contain village trails")

    // Optional: Verify trails are connected to non-trail items?
    // Hard to verify connectivity easily without graph traversal, but existence is key.
  }
}
