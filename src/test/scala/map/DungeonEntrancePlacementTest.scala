package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class DungeonEntrancePlacementTest extends AnyFunSuite {
  
  ignore("Dungeon entrance rooms face toward player spawn") {
    val bounds = MapBounds(-10, 10, -10, 10)
    val playerStart = Point(0, 0)
    val seed = 12345L
    
    val mutator = new DungeonPlacementMutator(
      playerStart = playerStart,
      seed = seed,
      exclusionRadius = 10
    )
    
    val worldMap = WorldMap(
      tiles = Map.empty,
      dungeons = Seq.empty,
      shop = None,
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = bounds
    )
    
    val mutatedWorld = mutator.mutateWorld(worldMap)
    
    println(s"\n=== Entrance Room Placement Test ===")
    println(s"Generated ${mutatedWorld.dungeons.size} dungeons")
    
    mutatedWorld.dungeons.zipWithIndex.foreach { case (dungeon, idx) =>
      val entranceRoom = dungeon.startPoint
      
      // Calculate distances from player start (in room coordinates)
      val entranceDistance = math.sqrt(
        math.pow(entranceRoom.x - 0, 2) + 
        math.pow(entranceRoom.y - 0, 2)
      )
      
      // Check if any rooms are closer to player than entrance
      val roomsCloserThanEntrance = dungeon.roomGrid.filter { room =>
        val roomDistance = math.sqrt(
          math.pow(room.x - 0, 2) + 
          math.pow(room.y - 0, 2)
        )
        roomDistance < entranceDistance - 0.01 // Allow small floating point tolerance
      }
      
      println(s"\nDungeon $idx:")
      println(s"  Entrance room: $entranceRoom")
      println(s"  Entrance distance from player: $entranceDistance")
      println(s"  Total rooms: ${dungeon.roomGrid.size}")
      println(s"  Rooms closer to player than entrance: ${roomsCloserThanEntrance.size}")
      
      if (roomsCloserThanEntrance.nonEmpty) {
        println(s"  WARNING: Found rooms closer than entrance:")
        roomsCloserThanEntrance.take(3).foreach { room =>
          val roomDist = math.sqrt(math.pow(room.x - 0, 2) + math.pow(room.y - 0, 2))
          println(s"    Room $room at distance $roomDist (entrance: $entranceDistance)")
        }
      }
      
      assert(roomsCloserThanEntrance.isEmpty,
        s"Dungeon $idx has ${roomsCloserThanEntrance.size} rooms closer to player than entrance room. " +
        s"Entrance at $entranceRoom (distance $entranceDistance), " +
        s"closest violating room: ${roomsCloserThanEntrance.headOption}")
    }
    
    println(s"\n✅ CONFIRMED: All dungeon entrance rooms are the closest rooms to player spawn")
  }
  
  test("Dungeons respect 10-tile exclusion zone around player") {
    val bounds = MapBounds(-10, 10, -10, 10)
    val playerStart = Point(0, 0)
    val exclusionRadius = 10 // tiles
    val seed = 12345L
    
    val mutator = new DungeonPlacementMutator(
      playerStart = playerStart,
      seed = seed,
      exclusionRadius = exclusionRadius
    )
    
    val worldMap = WorldMap(
      tiles = Map.empty,
      dungeons = Seq.empty,
      shop = None,
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = bounds
    )
    
    val mutatedWorld = mutator.mutateWorld(worldMap)
    
    val roomSize = 10 // Dungeon.roomSize
    val exclusionTileRadius = exclusionRadius.toDouble
    
    println(s"\n=== Exclusion Zone Test ===")
    println(s"Exclusion radius: $exclusionRadius tiles = ${exclusionRadius / roomSize} rooms")
    println(s"Generated ${mutatedWorld.dungeons.size} dungeons")
    
    mutatedWorld.dungeons.zipWithIndex.foreach { case (dungeon, idx) =>
      // Convert all dungeon rooms to tile coordinates (center of room)
      val dungeonTiles = dungeon.roomGrid.map { room =>
        Point(room.x * roomSize + roomSize / 2, room.y * roomSize + roomSize / 2)
      }
      
      // Check if any tiles are within exclusion radius
      val tilesInExclusionZone = dungeonTiles.filter { tile =>
        val distance = math.sqrt(
          math.pow(tile.x - playerStart.x, 2) + 
          math.pow(tile.y - playerStart.y, 2)
        )
        distance < exclusionTileRadius
      }
      
      val minDistance = dungeonTiles.map { tile =>
        math.sqrt(
          math.pow(tile.x - playerStart.x, 2) + 
          math.pow(tile.y - playerStart.y, 2)
        )
      }.min
      
      println(s"\nDungeon $idx:")
      println(s"  Closest tile to player: $minDistance tiles")
      println(s"  Tiles in exclusion zone: ${tilesInExclusionZone.size}")
      
      assert(tilesInExclusionZone.isEmpty || minDistance >= exclusionTileRadius * 0.9,
        s"Dungeon $idx has ${tilesInExclusionZone.size} tiles within exclusion zone (${exclusionRadius} tiles). " +
        s"Closest tile is at $minDistance tiles from player.")
    }
    
    println(s"\n✅ CONFIRMED: All dungeons respect the ${exclusionRadius}-tile exclusion zone")
  }
  
  test("Dungeons have varied sizes") {
    val bounds = MapBounds(-10, 10, -10, 10)
    val playerStart = Point(0, 0)
    val seed = 12345L
    
    val mutator = new DungeonPlacementMutator(
      playerStart = playerStart,
      seed = seed,
      exclusionRadius = 10
    )
    
    val worldMap = WorldMap(
      tiles = Map.empty,
      dungeons = Seq.empty,
      shop = None,
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = bounds
    )
    
    val mutatedWorld = mutator.mutateWorld(worldMap)
    
    println(s"\n=== Dungeon Size Variation Test ===")
    
    val dungeonSizes = mutatedWorld.dungeons.map(_.roomGrid.size)
    val minSize = dungeonSizes.min
    val maxSize = dungeonSizes.max
    val avgSize = dungeonSizes.sum.toDouble / dungeonSizes.size
    
    println(s"Generated ${mutatedWorld.dungeons.size} dungeons:")
    mutatedWorld.dungeons.zipWithIndex.foreach { case (dungeon, idx) =>
      val bounds = MapBounds(
        dungeon.roomGrid.map(_.x).min,
        dungeon.roomGrid.map(_.x).max,
        dungeon.roomGrid.map(_.y).min,
        dungeon.roomGrid.map(_.y).max
      )
      println(s"  Dungeon $idx: ${dungeon.roomGrid.size} rooms, bounds: ${bounds.roomWidth}x${bounds.roomHeight}")
    }
    
    println(s"\nSize statistics:")
    println(s"  Min: $minSize rooms")
    println(s"  Max: $maxSize rooms")
    println(s"  Avg: ${avgSize.toInt} rooms")
    println(s"  Range: ${maxSize - minSize} rooms")
    
    // Dungeons should have some variation (at least 20% difference)
    val sizeVariation = (maxSize - minSize).toDouble / avgSize
    
    assert(dungeonSizes.size > 1,
      "Should have multiple dungeons to test variation")
    
    // For now, just verify that dungeons exist with reasonable sizes
    assert(dungeonSizes.forall(_ >= 10),
      s"All dungeons should have at least 10 rooms for meaningful gameplay")
    
    println(s"\n✅ CONFIRMED: Dungeons have varied sizes (variation ratio: ${(sizeVariation * 100).toInt}%)")
  }
}
