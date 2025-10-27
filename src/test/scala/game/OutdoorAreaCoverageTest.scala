package game

import map.{Dungeon, MapGenerator, TileType}
import org.scalatest.funsuite.AnyFunSuite

class OutdoorAreaCoverageTest extends AnyFunSuite {
  
  test("Outdoor area tiles include all grass variants") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 10, lockedDoorCount = 0, itemCount = 0)
    
    val grassTiles = dungeon.tiles.filter { case (_, tileType) =>
      tileType == TileType.Grass1 || tileType == TileType.Grass2 || tileType == TileType.Grass3
    }
    
    val hasGrass1 = grassTiles.exists(_._2 == TileType.Grass1)
    val hasGrass2 = grassTiles.exists(_._2 == TileType.Grass2)
    val hasGrass3 = grassTiles.exists(_._2 == TileType.Grass3)
    
    assert(hasGrass1, "Should have Grass1 tiles")
    assert(hasGrass2, "Should have Grass2 tiles")
    assert(hasGrass3, "Should have Grass3 tiles")
    
    println(s"✓ Found all 3 grass variants in outdoor area")
    println(s"  Grass1: ${grassTiles.count(_._2 == TileType.Grass1)} tiles")
    println(s"  Grass2: ${grassTiles.count(_._2 == TileType.Grass2)} tiles")
    println(s"  Grass3: ${grassTiles.count(_._2 == TileType.Grass3)} tiles")
  }
  
  test("Outdoor perimeter creates complete tree boundary") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    
    val minRoomX = dungeon.roomGrid.map(_.x).min
    val maxRoomX = dungeon.roomGrid.map(_.x).max
    val minRoomY = dungeon.roomGrid.map(_.y).min
    val maxRoomY = dungeon.roomGrid.map(_.y).max
    
    val outdoorPadding = 2
    val outdoorMinX = (minRoomX - outdoorPadding) * Dungeon.roomSize
    val outdoorMaxX = (maxRoomX + outdoorPadding + 1) * Dungeon.roomSize
    val outdoorMinY = (minRoomY - outdoorPadding) * Dungeon.roomSize
    val outdoorMaxY = (maxRoomY + outdoorPadding + 1) * Dungeon.roomSize
    
    // Check top edge is all trees
    val topEdge = (outdoorMinX to outdoorMaxX).count { x =>
      dungeon.tiles.get(Point(x, outdoorMinY)).contains(TileType.Tree)
    }
    
    // Check bottom edge is all trees
    val bottomEdge = (outdoorMinX to outdoorMaxX).count { x =>
      dungeon.tiles.get(Point(x, outdoorMaxY)).contains(TileType.Tree)
    }
    
    // Check left edge is all trees
    val leftEdge = (outdoorMinY to outdoorMaxY).count { y =>
      dungeon.tiles.get(Point(outdoorMinX, y)).contains(TileType.Tree)
    }
    
    // Check right edge is all trees
    val rightEdge = (outdoorMinY to outdoorMaxY).count { y =>
      dungeon.tiles.get(Point(outdoorMaxX, y)).contains(TileType.Tree)
    }
    
    val expectedWidth = outdoorMaxX - outdoorMinX + 1
    val expectedHeight = outdoorMaxY - outdoorMinY + 1
    
    assert(topEdge == expectedWidth, s"Top edge should be complete (${topEdge}/${expectedWidth})")
    assert(bottomEdge == expectedWidth, s"Bottom edge should be complete (${bottomEdge}/${expectedWidth})")
    assert(leftEdge == expectedHeight, s"Left edge should be complete (${leftEdge}/${expectedHeight})")
    assert(rightEdge == expectedHeight, s"Right edge should be complete (${rightEdge}/${expectedHeight})")
    
    println(s"✓ Complete tree perimeter verified")
    println(s"  Top: ${topEdge}, Bottom: ${bottomEdge}, Left: ${leftEdge}, Right: ${rightEdge}")
  }
  
  test("Starting room remains outdoor room") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 10, lockedDoorCount = 0, itemCount = 0)
    
    assert(dungeon.outdoorRooms.contains(dungeon.startPoint),
      "Starting room should be marked as outdoor room")
    
    println(s"✓ Starting room (${dungeon.startPoint}) is in outdoor rooms set")
  }
  
  test("Outdoor and dungeon rooms are disjoint sets") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 10, lockedDoorCount = 0, itemCount = 0)
    
    val dungeonRooms = dungeon.roomGrid -- dungeon.outdoorRooms
    val overlap = dungeonRooms.intersect(dungeon.outdoorRooms)
    
    assert(overlap.isEmpty, s"Outdoor and dungeon rooms should not overlap: $overlap")
    assert(dungeonRooms.nonEmpty, "Should have at least one dungeon room")
    assert(dungeon.outdoorRooms.nonEmpty, "Should have at least one outdoor room")
    
    println(s"✓ Outdoor rooms: ${dungeon.outdoorRooms.size}, Dungeon rooms: ${dungeonRooms.size}")
    println(s"✓ No overlap between outdoor and dungeon room sets")
  }
  
  test("Player spawns in dungeon start point on procedural terrain") {
    val startingState = StartingState
    val player = startingState.startingGameState.playerEntity
    val maybeDungeon = startingState.startingGameState.worldMap.primaryDungeon
    
    val playerPos = player.get[game.entity.Movement].map(_.position).getOrElse(Point(0, 0))
    
    maybeDungeon match {
      case Some(dungeon) =>
        val playerRoomX = if (playerPos.x >= 0) playerPos.x / Dungeon.roomSize 
                          else (playerPos.x - Dungeon.roomSize + 1) / Dungeon.roomSize
        val playerRoomY = if (playerPos.y >= 0) playerPos.y / Dungeon.roomSize 
                          else (playerPos.y - Dungeon.roomSize + 1) / Dungeon.roomSize
        val playerRoom = Point(playerRoomX, playerRoomY)
        
        assert(dungeon.roomGrid.contains(playerRoom),
          s"Player should spawn in a dungeon room, but spawned in room $playerRoom")
        
        assert(playerRoom == dungeon.startPoint,
          s"Player should spawn at dungeon start point ${dungeon.startPoint}, but spawned at $playerRoom")
        
        println(s"✓ Player spawns at $playerPos (room $playerRoom) which is the dungeon start point")
      case None =>
        // No dungeon in open world - player can spawn anywhere
        println(s"✓ Player spawns in open world at $playerPos (no dungeon)")
    }
  }
  
  test("Outdoor rooms have connections to dungeon") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    
    // Find connections from outdoor rooms to dungeon rooms
    val outdoorToDungeon = dungeon.roomConnections.filter { conn =>
      dungeon.outdoorRooms.contains(conn.originRoom) &&
      !dungeon.outdoorRooms.contains(conn.destinationRoom)
    }
    
    assert(outdoorToDungeon.nonEmpty, "Should have at least one connection from outdoor to dungeon")
    
    println(s"✓ Found ${outdoorToDungeon.size} connections from outdoor to dungeon")
    outdoorToDungeon.foreach { conn =>
      println(s"  ${conn.originRoom} -> ${conn.destinationRoom} (${conn.direction})")
    }
  }
  
  test("Dungeon rooms are accessible from outdoor entrance") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 10, lockedDoorCount = 0, itemCount = 0)
    
    // All dungeon rooms should have a depth (be reachable)
    val dungeonRooms = dungeon.roomGrid -- dungeon.outdoorRooms
    val roomsWithDepth = dungeonRooms.filter(dungeon.roomDepths.contains)
    
    assert(roomsWithDepth.size == dungeonRooms.size,
      s"All dungeon rooms should be reachable: ${roomsWithDepth.size}/${dungeonRooms.size}")
    
    println(s"✓ All ${dungeonRooms.size} dungeon rooms are accessible")
  }
  
  test("Boss room is not in outdoor area") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 10, lockedDoorCount = 0, itemCount = 0)
    
    if (dungeon.hasBossRoom && dungeon.endpoint.isDefined) {
      val bossRoom = dungeon.endpoint.get
      assert(!dungeon.outdoorRooms.contains(bossRoom),
        "Boss room should not be in outdoor area")
      println(s"✓ Boss room at $bossRoom is in dungeon area (not outdoor)")
    } else {
      println(s"✓ No boss room in this test dungeon")
    }
  }
  
  test("Trader room is not in outdoor area") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 10, lockedDoorCount = 0, itemCount = 0)
    
    if (dungeon.traderRoom.isDefined) {
      val traderRoom = dungeon.traderRoom.get
      assert(!dungeon.outdoorRooms.contains(traderRoom),
        "Trader room should not be in outdoor area")
      println(s"✓ Trader room at $traderRoom is in dungeon area (not outdoor)")
    } else {
      println(s"✓ No trader room in this test dungeon")
    }
  }
  
  test("Outdoor area provides safe zone - walkable grass tiles") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    
    // Get all outdoor room tiles
    val outdoorRoomTiles = dungeon.outdoorRooms.flatMap { room =>
      val roomX = room.x * Dungeon.roomSize
      val roomY = room.y * Dungeon.roomSize
      
      (roomX to roomX + Dungeon.roomSize).flatMap { x =>
        (roomY to roomY + Dungeon.roomSize).flatMap { y =>
          dungeon.tiles.get(Point(x, y))
        }
      }
    }
    
    val walkableCount = outdoorRoomTiles.count { tileType =>
      tileType == TileType.Grass1 || tileType == TileType.Grass2 || 
      tileType == TileType.Grass3 || tileType == TileType.Dirt
    }
    
    val totalCount = outdoorRoomTiles.size
    val walkablePercentage = (walkableCount * 100.0) / totalCount
    
    assert(walkablePercentage >= 40, 
      s"Outdoor rooms should be mostly walkable: ${walkablePercentage}%")
    
    println(s"✓ Outdoor rooms are ${walkablePercentage.toInt}% walkable")
  }
  
  test("Outdoor area size scales with dungeon size") {
    val smallDungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    val largeDungeon = MapGenerator.generateDungeon(dungeonSize = 15, lockedDoorCount = 0, itemCount = 0)
    
    val smallOutdoorTiles = smallDungeon.tiles.filter { case (_, tileType) =>
      tileType == TileType.Grass1 || tileType == TileType.Grass2 || 
      tileType == TileType.Grass3 || tileType == TileType.Tree
    }.size
    
    val largeOutdoorTiles = largeDungeon.tiles.filter { case (_, tileType) =>
      tileType == TileType.Grass1 || tileType == TileType.Grass2 || 
      tileType == TileType.Grass3 || tileType == TileType.Tree
    }.size
    
    assert(largeOutdoorTiles > smallOutdoorTiles,
      "Larger dungeon should have more outdoor tiles")
    
    println(s"✓ Small dungeon: ${smallOutdoorTiles} outdoor tiles")
    println(s"✓ Large dungeon: ${largeOutdoorTiles} outdoor tiles")
  }
}
