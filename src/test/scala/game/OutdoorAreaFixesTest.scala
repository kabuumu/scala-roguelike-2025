package game

import map.{Dungeon, MapGenerator, TileType}
import org.scalatest.funsuite.AnyFunSuite

class OutdoorAreaFixesTest extends AnyFunSuite {
  
  test("No enemies spawn in outdoor rooms") {
    val startingState = StartingState
    val enemies = startingState.enemies
    val maybeDungeon = startingState.startingGameState.worldMap.primaryDungeon
    
    maybeDungeon.foreach { dungeon =>
      // Check that no enemies are in any outdoor rooms
      val enemiesInOutdoor = enemies.filter { enemy =>
        enemy.get[game.entity.Movement].exists { movement =>
          val pos = movement.position
          // Calculate which room this position is in
          // Note: Room coordinates are calculated by dividing pixel coordinates by room size
          // and we need to use floor division for negative coordinates
          val roomX = if (pos.x >= 0) pos.x / Dungeon.roomSize else (pos.x - Dungeon.roomSize + 1) / Dungeon.roomSize
          val roomY = if (pos.y >= 0) pos.y / Dungeon.roomSize else (pos.y - Dungeon.roomSize + 1) / Dungeon.roomSize
          val roomPoint = Point(roomX, roomY)
          dungeon.outdoorRooms.contains(roomPoint)
        }
      }
      
      if (enemiesInOutdoor.nonEmpty) {
        println(s"Enemies in outdoor rooms:")
        enemiesInOutdoor.foreach { enemy =>
          val pos = enemy.get[game.entity.Movement].map(_.position).getOrElse(Point(0, 0))
          val roomX = if (pos.x >= 0) pos.x / Dungeon.roomSize else (pos.x - Dungeon.roomSize + 1) / Dungeon.roomSize
          val roomY = if (pos.y >= 0) pos.y / Dungeon.roomSize else (pos.y - Dungeon.roomSize + 1) / Dungeon.roomSize
          println(s"  ${enemy.id} at $pos -> room ($roomX, $roomY)")
        }
      }
      
      assert(enemiesInOutdoor.isEmpty, s"No enemies should spawn in outdoor rooms, but found ${enemiesInOutdoor.size}")
      println(s"✓ No enemies in outdoor rooms (total enemies: ${enemies.size})")
    }
  }
  
  test("Outdoor rooms do not have dungeon depth") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    
    // Check that outdoor rooms are not in the roomDepths map
    val outdoorRoomsWithDepth = dungeon.outdoorRooms.filter(room => 
      dungeon.roomDepths.contains(room)
    )
    
    assert(outdoorRoomsWithDepth.isEmpty, 
      s"Outdoor rooms should not have dungeon depth, but found: $outdoorRoomsWithDepth")
    
    println(s"✓ Outdoor rooms have no depth (${dungeon.outdoorRooms.size} outdoor rooms)")
    println(s"✓ Dungeon rooms with depth: ${dungeon.roomDepths.size}")
  }
  
  test("Solid wall exists between outdoor and dungeon areas with only doorway access") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    
    // Find the connection between outdoor entrance and dungeon entrance
    val outdoorToDungeonConnection = dungeon.roomConnections.find { conn =>
      dungeon.outdoorRooms.contains(conn.originRoom) && 
      !dungeon.outdoorRooms.contains(conn.destinationRoom)
    }
    
    assert(outdoorToDungeonConnection.isDefined, "Should have connection from outdoor to dungeon")
    
    val connection = outdoorToDungeonConnection.get
    val outdoorRoom = connection.originRoom
    val dungeonRoom = connection.destinationRoom
    
    println(s"✓ Connection found: outdoor room $outdoorRoom -> dungeon room $dungeonRoom")
    
    // Check the wall tiles around this connection
    val outdoorRoomX = outdoorRoom.x * Dungeon.roomSize
    val outdoorRoomY = outdoorRoom.y * Dungeon.roomSize
    
    // Get the wall where the connection is
    val wallTiles = connection.direction match {
      case game.Direction.Up =>
        // Wall is on top of outdoor room
        (outdoorRoomX to outdoorRoomX + Dungeon.roomSize).map { x =>
          Point(x, outdoorRoomY)
        }
      case game.Direction.Down =>
        (outdoorRoomX to outdoorRoomX + Dungeon.roomSize).map { x =>
          Point(x, outdoorRoomY + Dungeon.roomSize)
        }
      case game.Direction.Left =>
        (outdoorRoomY to outdoorRoomY + Dungeon.roomSize).map { y =>
          Point(outdoorRoomX, y)
        }
      case game.Direction.Right =>
        (outdoorRoomY to outdoorRoomY + Dungeon.roomSize).map { y =>
          Point(outdoorRoomX + Dungeon.roomSize, y)
        }
    }
    
    // Count wall tiles (impassable) vs door tiles (passable)
    val wallCount = wallTiles.count { point =>
      dungeon.tiles.get(point).exists(t => t == TileType.Wall || t == TileType.Tree)
    }
    
    val doorCount = wallTiles.count { point =>
      dungeon.doorPoints.contains(point)
    }
    
    println(s"✓ Wall tiles: $wallCount, Door tiles: $doorCount")
    assert(wallCount > 0, "Should have solid wall tiles")
    assert(doorCount > 0, "Should have at least one door tile")
    assert(wallCount > doorCount, "Wall tiles should outnumber door tiles (mostly solid wall)")
    
    // Check that the door tile is actually a floor type (allowing for different floor variations)
    val doorTile = wallTiles.find(dungeon.doorPoints.contains).flatMap(dungeon.tiles.get)
    println(s"✓ Door tile type: $doorTile")
    val acceptableFloorTypes = Set(TileType.Floor, TileType.MaybeFloor, TileType.Bridge)
    assert(doorTile.exists(acceptableFloorTypes.contains), 
      s"Door between outdoor and dungeon should be a walkable floor type, but was $doorTile")
  }
}

