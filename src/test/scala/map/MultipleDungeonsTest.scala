package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class MultipleDungeonsTest extends AnyFunSuite {
  
  test("World map of 21x21 (-10 to 10) generates 4 dungeons") {
    val bounds = MapBounds(-10, 10, -10, 10)
    val config = WorldMapConfig(
      worldConfig = WorldConfig(bounds, seed = 12345)
    )
    
    val worldMap = WorldMapGenerator.generateWorldMap(config)
    
    println(s"\n=== Multiple Dungeons Test ===")
    println(s"World bounds: ${bounds.describe}")
    println(s"Number of dungeons: ${worldMap.dungeons.size}")
    
    assert(worldMap.dungeons.size == 4, 
      s"Expected 4 dungeons for 21x21 world, got ${worldMap.dungeons.size}")
    
    // Verify each dungeon has reasonable bounds
    worldMap.dungeons.zipWithIndex.foreach { case (dungeon, idx) =>
      val roomGrid = dungeon.roomGrid
      val minX = roomGrid.map(_.x).min
      val maxX = roomGrid.map(_.x).max
      val minY = roomGrid.map(_.y).min
      val maxY = roomGrid.map(_.y).max
      
      val dungeonBounds = MapBounds(minX, maxX, minY, maxY)
      
      println(s"\nDungeon $idx:")
      println(s"  Bounds: ${dungeonBounds.describe}")
      println(s"  Rooms: ${roomGrid.size}")
      println(s"  Start point: ${dungeon.startPoint}")
      
      assert(roomGrid.nonEmpty, s"Dungeon $idx should have rooms")
      // Dungeons should have at least 10 rooms to be meaningful
      assert(roomGrid.size >= 10, 
        s"Dungeon $idx should have at least 10 rooms, got ${roomGrid.size}")
    }
    
    // Verify dungeons don't overlap by checking room positions
    val allRoomPositions = worldMap.dungeons.flatMap(_.roomGrid).toSet
    val totalRooms = worldMap.dungeons.map(_.roomGrid.size).sum
    
    println(s"\nTotal rooms across all dungeons: $totalRooms")
    println(s"Unique room positions: ${allRoomPositions.size}")
    
    // Some overlap is okay since dungeons may share boundary rooms,
    // but they should mostly be distinct
    val overlapPercent = (1.0 - allRoomPositions.size.toDouble / totalRooms) * 100
    println(s"Room overlap: ${overlapPercent.toInt}%")
    
    assert(overlapPercent < 10, 
      s"Dungeons should have minimal overlap, got ${overlapPercent.toInt}%")
    
    println(s"\n✅ CONFIRMED: 4 dungeons generated for 21x21 world with minimal overlap")
  }
  

}
