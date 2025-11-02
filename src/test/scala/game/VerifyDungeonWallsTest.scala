package game

import org.scalatest.funsuite.AnyFunSuite
import map.TileType

class VerifyDungeonWallsTest extends AnyFunSuite {
  test("Verify dungeon has wall tiles in worldMap") {
    println(s"\n=== Dungeon Wall Verification ===")
    
    val state = StartingState.startingGameState
    val worldMap = state.worldMap
    
    println(s"Total tiles in worldMap: ${worldMap.tiles.size}")
    
    // Get the primary dungeon
    assert(worldMap.primaryDungeon.isDefined, "Primary dungeon should exist")
    val dungeon = worldMap.primaryDungeon.get
    
    println(s"\nDungeon structure:")
    println(s"  Rooms: ${dungeon.roomGrid.size}")
    println(s"  Dungeon.tiles size: ${dungeon.tiles.size}")
    println(s"  Dungeon.walls size: ${dungeon.walls.size}")
    
    // Check that the dungeon itself has wall tiles
    assert(dungeon.walls.nonEmpty, "Dungeon should have walls in its walls set")
    
    // Check that dungeon tiles include Wall type
    val wallTilesInDungeon = dungeon.tiles.filter(_._2 == TileType.Wall)
    println(s"  Wall tiles in dungeon.tiles: ${wallTilesInDungeon.size}")
    assert(wallTilesInDungeon.nonEmpty, "Dungeon.tiles should contain Wall tiles")
    
    // Now check if these walls are in the worldMap
    val wallTilesInWorldMap = worldMap.tiles.filter(_._2 == TileType.Wall)
    println(s"\nWall tiles in worldMap: ${wallTilesInWorldMap.size}")
    
    // Verify that walls from the dungeon are in the worldMap
    val dungeonWallsInWorldMap = dungeon.walls.count(wall => 
      worldMap.tiles.get(wall).contains(TileType.Wall)
    )
    println(s"Dungeon walls present in worldMap: $dungeonWallsInWorldMap / ${dungeon.walls.size}")
    
    // Print some sample wall positions
    println(s"\nSample dungeon wall positions (first 5):")
    dungeon.walls.take(5).foreach { wall =>
      val inWorldMap = worldMap.tiles.get(wall)
      println(s"  $wall -> ${inWorldMap.getOrElse("NOT IN WORLDMAP")}")
    }
    
    // Print dungeon room bounds so we know where to look
    val minX = dungeon.roomGrid.map(_.x).min
    val maxX = dungeon.roomGrid.map(_.x).max
    val minY = dungeon.roomGrid.map(_.y).min
    val maxY = dungeon.roomGrid.map(_.y).max
    println(s"\nDungeon room grid bounds: rooms ($minX,$minY) to ($maxX,$maxY)")
    println(s"Dungeon tile bounds (approximate): tiles (${minX * 11},${minY * 11}) to (${maxX * 11 + 11},${maxY * 11 + 11})")
    
    assert(wallTilesInWorldMap.nonEmpty, "WorldMap should contain Wall tiles from the dungeon")
    assert(dungeonWallsInWorldMap > 0, s"At least some dungeon walls should be present in worldMap (found $dungeonWallsInWorldMap)")
    
    println(s"\n✅ CONFIRMED: Dungeon has ${dungeonWallsInWorldMap} wall tiles in the worldMap")
  }
}

