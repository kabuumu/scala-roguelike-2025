package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class WorldGeneratorTest extends AnyFunSuite {
  
  test("generateWorld creates tiles within bounds") {
    val bounds = MapBounds(0, 3, 0, 3)
    val config = WorldConfig(bounds, seed = 12345)
    
    val tiles = WorldGenerator.generateWorld(config)
    
    // Check all tiles are within expected tile bounds
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    
    tiles.keys.foreach { point =>
      assert(point.x >= tileMinX && point.x <= tileMaxX, s"X coordinate $point out of bounds")
      assert(point.y >= tileMinY && point.y <= tileMaxY, s"Y coordinate $point out of bounds")
    }
    
    // Should generate correct number of tiles
    val expectedTiles = (tileMaxX - tileMinX + 1) * (tileMaxY - tileMinY + 1)
    assert(tiles.size == expectedTiles, s"Expected $expectedTiles tiles, got ${tiles.size}")
    
    println(s"Generated ${tiles.size} tiles within bounds")
  }
  
  test("generateWorld respects grass density configuration") {
    val bounds = MapBounds(0, 5, 0, 5)
    val config = WorldConfig(bounds, grassDensity = 0.9, treeDensity = 0.05, dirtDensity = 0.05, 
                             perimeterTrees = false, seed = 12345)
    
    val tiles = WorldGenerator.generateWorld(config)
    
    val grassCount = tiles.values.count {
      case TileType.Grass1 | TileType.Grass2 | TileType.Grass3 => true
      case _ => false
    }
    
    val grassPercent = grassCount.toDouble / tiles.size
    
    // Should be close to configured density (within 20% tolerance due to randomness and noise)
    assert(grassPercent >= 0.7 && grassPercent <= 1.0, 
           s"Grass density $grassPercent not close to configured 0.9")
    
    println(s"Grass density: ${(grassPercent * 100).toInt}% (configured: 90%)")
  }
  
  test("generateWorld creates perimeter of trees when enabled") {
    val bounds = MapBounds(0, 3, 0, 3)
    val config = WorldConfig(bounds, perimeterTrees = true, seed = 12345)
    
    val tiles = WorldGenerator.generateWorld(config)
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    
    // Check all perimeter tiles are trees
    val perimeterTiles = tiles.filter { case (point, _) =>
      point.x == tileMinX || point.x == tileMaxX || point.y == tileMinY || point.y == tileMaxY
    }
    
    perimeterTiles.foreach { case (point, tileType) =>
      assert(tileType == TileType.Tree, s"Perimeter tile at $point should be Tree, got $tileType")
    }
    
    println(s"Verified ${perimeterTiles.size} perimeter tiles are trees")
  }
  
  test("generateWorld without perimeter trees allows walkable border") {
    val bounds = MapBounds(0, 3, 0, 3)
    val config = WorldConfig(bounds, perimeterTrees = false, grassDensity = 1.0, 
                             treeDensity = 0.0, dirtDensity = 0.0, seed = 12345)
    
    val tiles = WorldGenerator.generateWorld(config)
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    
    // Check that perimeter has some walkable tiles
    val perimeterTiles = tiles.filter { case (point, _) =>
      point.x == tileMinX || point.x == tileMaxX || point.y == tileMinY || point.y == tileMaxY
    }
    
    val walkablePerimeter = perimeterTiles.filter { case (_, tileType) =>
      tileType != TileType.Tree && tileType != TileType.Wall
    }
    
    assert(walkablePerimeter.nonEmpty, "Should have walkable perimeter tiles when perimeterTrees is false")
    
    println(s"Walkable perimeter tiles: ${walkablePerimeter.size}/${perimeterTiles.size}")
  }
  
  test("generateWorld includes grass variants for visual variety") {
    val bounds = MapBounds(0, 5, 0, 5)
    val config = WorldConfig(bounds, grassDensity = 0.7, treeDensity = 0.1, dirtDensity = 0.1, seed = 12345)
    
    val tiles = WorldGenerator.generateWorld(config)
    
    val hasGrass1 = tiles.values.exists(_ == TileType.Grass1)
    val hasGrass2 = tiles.values.exists(_ == TileType.Grass2)
    val hasGrass3 = tiles.values.exists(_ == TileType.Grass3)
    
    assert(hasGrass1, "Should have Grass1 tiles")
    assert(hasGrass2, "Should have Grass2 tiles")
    assert(hasGrass3, "Should have Grass3 tiles")
    
    println("World has all grass variants for visual variety")
  }
  
  test("generateWorld creates trees when tree density is set") {
    val bounds = MapBounds(0, 5, 0, 5)
    val config = WorldConfig(bounds, grassDensity = 0.5, treeDensity = 0.3, perimeterTrees = false, seed = 12345)
    
    val tiles = WorldGenerator.generateWorld(config)
    
    val treeCount = tiles.values.count(_ == TileType.Tree)
    
    assert(treeCount > 0, "Should have some tree tiles with treeDensity = 0.3")
    
    val treePercent = treeCount.toDouble / tiles.size
    println(s"Tree density: ${(treePercent * 100).toInt}% (configured: 30%)")
  }
  
  test("generateWorld creates dirt tiles when dirt density is set") {
    val bounds = MapBounds(0, 5, 0, 5)
    val config = WorldConfig(bounds, grassDensity = 0.5, treeDensity = 0.0, dirtDensity = 0.3, 
                             perimeterTrees = false, seed = 12345)
    
    val tiles = WorldGenerator.generateWorld(config)
    
    val dirtCount = tiles.values.count(_ == TileType.Dirt)
    
    assert(dirtCount > 0, "Should have some dirt tiles with dirtDensity = 0.3")
    
    val dirtPercent = dirtCount.toDouble / tiles.size
    println(s"Dirt density: ${(dirtPercent * 100).toInt}% (configured: 30%)")
  }
  
  test("ensureWalkablePaths makes center area walkable") {
    val bounds = MapBounds(0, 5, 0, 5)
    val config = WorldConfig(bounds, grassDensity = 0.1, treeDensity = 0.8, 
                             ensureWalkablePaths = true, perimeterTrees = false, seed = 12345)
    
    val tiles = WorldGenerator.generateWorld(config)
    val (tileMinX, tileMaxX, tileMinY, tileMaxY) = bounds.toTileBounds()
    
    // Check center area is walkable
    val centerX = (tileMinX + tileMaxX) / 2
    val centerY = (tileMinY + tileMaxY) / 2
    
    val centerTile = tiles.get(Point(centerX, centerY))
    assert(centerTile.exists(_ != TileType.Tree), s"Center tile should be walkable, got $centerTile")
    
    println(s"Center tile at ($centerX, $centerY): $centerTile")
  }
  
  test("describeWorld provides AI-readable output") {
    val bounds = MapBounds(0, 3, 0, 3)
    val config = WorldConfig(bounds, grassDensity = 0.6, treeDensity = 0.2, dirtDensity = 0.1, seed = 12345)
    
    val tiles = WorldGenerator.generateWorld(config)
    val description = WorldGenerator.describeWorld(tiles, config)
    
    println("\n" + description + "\n")
    
    assert(description.contains("Bounds"))
    assert(description.contains("Total tiles"))
    assert(description.contains("Grass tiles"))
    assert(description.contains("Tree tiles"))
    assert(description.contains("Dirt tiles"))
  }
  
  test("generateWorld is deterministic with same seed") {
    val bounds = MapBounds(0, 3, 0, 3)
    val config1 = WorldConfig(bounds, seed = 99999)
    val config2 = WorldConfig(bounds, seed = 99999)
    
    val tiles1 = WorldGenerator.generateWorld(config1)
    val tiles2 = WorldGenerator.generateWorld(config2)
    
    assert(tiles1 == tiles2, "Same seed should produce identical worlds")
    
    println("Deterministic generation verified with seed 99999")
  }
  
  test("generateWorld produces different results with different seeds") {
    val bounds = MapBounds(0, 3, 0, 3)
    val config1 = WorldConfig(bounds, seed = 11111)
    val config2 = WorldConfig(bounds, seed = 22222)
    
    val tiles1 = WorldGenerator.generateWorld(config1)
    val tiles2 = WorldGenerator.generateWorld(config2)
    
    assert(tiles1 != tiles2, "Different seeds should produce different worlds")
    
    println("Different seeds produce different terrain patterns")
  }
  
  test("generateWorld with high grass density creates mostly walkable terrain") {
    val bounds = MapBounds(0, 5, 0, 5)
    val config = WorldConfig(bounds, grassDensity = 0.85, treeDensity = 0.1, dirtDensity = 0.05, 
                             perimeterTrees = true, seed = 12345)
    
    val tiles = WorldGenerator.generateWorld(config)
    
    val walkableTiles = tiles.values.count {
      case TileType.Grass1 | TileType.Grass2 | TileType.Grass3 | TileType.Dirt => true
      case _ => false
    }
    
    val walkablePercent = walkableTiles.toDouble / tiles.size
    
    // Most of the world should be walkable with high grass density
    assert(walkablePercent >= 0.7, s"Walkable percent $walkablePercent should be >= 70%")
    
    println(s"Walkable terrain: ${(walkablePercent * 100).toInt}%")
  }
}
