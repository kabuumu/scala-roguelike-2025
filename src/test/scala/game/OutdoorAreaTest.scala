package game

import map.{Dungeon, MapGenerator, TileType}
import org.scalatest.funsuite.AnyFunSuite

class OutdoorAreaTest extends AnyFunSuite {
  
  test("Starting room is an outdoor area with grass tiles") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    val startRoom = dungeon.startPoint
    
    // Get tiles in the starting room
    val roomX = startRoom.x * Dungeon.roomSize
    val roomY = startRoom.y * Dungeon.roomSize
    
    val startRoomTiles = dungeon.tiles.filter { case (point, _) =>
      point.x >= roomX && point.x <= roomX + Dungeon.roomSize &&
      point.y >= roomY && point.y <= roomY + Dungeon.roomSize
    }
    
    // Should have grass tiles in the outdoor area
    val grassTiles = startRoomTiles.filter { case (_, tileType) =>
      tileType == TileType.Grass1 || tileType == TileType.Grass2 || tileType == TileType.Grass3
    }
    
    assert(grassTiles.nonEmpty, "Starting room should have grass tiles")
    println(s"Found ${grassTiles.size} grass tiles in starting room")
  }
  
  test("Starting room is surrounded by trees") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    val startRoom = dungeon.startPoint
    
    // Get perimeter tiles of the starting room
    val roomX = startRoom.x * Dungeon.roomSize
    val roomY = startRoom.y * Dungeon.roomSize
    
    val perimeterTiles = dungeon.tiles.filter { case (point, _) =>
      (point.x == roomX || point.x == roomX + Dungeon.roomSize) &&
      point.y >= roomY && point.y <= roomY + Dungeon.roomSize ||
      (point.y == roomY || point.y == roomY + Dungeon.roomSize) &&
      point.x >= roomX && point.x <= roomX + Dungeon.roomSize
    }
    
    // Count trees on the perimeter (excluding doors)
    val treeTiles = perimeterTiles.filter { case (point, tileType) =>
      tileType == TileType.Tree && !dungeon.doorPoints.contains(point)
    }
    
    // Should have trees on the perimeter
    assert(treeTiles.nonEmpty, "Starting room perimeter should have trees")
    println(s"Found ${treeTiles.size} tree tiles on starting room perimeter")
  }
  
  test("Trees are impassable (part of walls set)") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    
    // Get all tree tiles
    val treeTiles = dungeon.tiles.filter(_._2 == TileType.Tree).keySet
    
    // All tree tiles should be in the walls set (impassable)
    treeTiles.foreach { treePoint =>
      assert(dungeon.walls.contains(treePoint), s"Tree at $treePoint should be impassable")
    }
    
    println(s"Verified ${treeTiles.size} trees are impassable")
  }
  
  test("No enemies spawn in outdoor area") {
    val startingState = StartingState
    val enemies = startingState.enemies
    val maybeDungeon = startingState.startingGameState.worldMap.primaryDungeon
    
    maybeDungeon.foreach { dungeon =>
      // Calculate starting room bounds
      val startRoom = dungeon.startPoint
      val roomX = startRoom.x * Dungeon.roomSize
      val roomY = startRoom.y * Dungeon.roomSize
      
      // Check that no enemies are in the starting room
      val enemiesInStartRoom = enemies.filter { enemy =>
        enemy.get[game.entity.Movement].exists { movement =>
          val pos = movement.position
          pos.x >= roomX && pos.x <= roomX + Dungeon.roomSize &&
          pos.y >= roomY && pos.y <= roomY + Dungeon.roomSize
        }
      }
      
      assert(enemiesInStartRoom.isEmpty, "No enemies should spawn in the outdoor starting area")
      println(s"Verified no enemies in starting room (total enemies: ${enemies.size})")
    }
  }
  
  test("Outdoor area uses different tile types than regular dungeon") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 10, lockedDoorCount = 0, itemCount = 0)
    val startRoom = dungeon.startPoint
    
    // Get tiles in starting room
    val roomX = startRoom.x * Dungeon.roomSize
    val roomY = startRoom.y * Dungeon.roomSize
    
    val startRoomTiles = dungeon.tiles.filter { case (point, _) =>
      point.x >= roomX && point.x <= roomX + Dungeon.roomSize &&
      point.y >= roomY && point.y <= roomY + Dungeon.roomSize
    }.values.toSet
    
    // Get tiles from a non-starting room
    val otherRoom = (dungeon.roomGrid - startRoom).head
    val otherRoomX = otherRoom.x * Dungeon.roomSize
    val otherRoomY = otherRoom.y * Dungeon.roomSize
    
    val otherRoomTiles = dungeon.tiles.filter { case (point, _) =>
      point.x >= otherRoomX && point.x <= otherRoomX + Dungeon.roomSize &&
      point.y >= otherRoomY && point.y <= otherRoomY + Dungeon.roomSize
    }.values.toSet
    
    // Starting room should have outdoor tiles (grass, trees)
    val hasOutdoorTiles = startRoomTiles.exists(t => 
      t == TileType.Grass1 || t == TileType.Grass2 || t == TileType.Grass3 || t == TileType.Tree
    )
    
    // Other rooms should have indoor tiles (floor, wall, etc.)
    val hasIndoorTiles = otherRoomTiles.exists(t => 
      t == TileType.Floor || t == TileType.Wall
    )
    
    assert(hasOutdoorTiles, "Starting room should have outdoor tiles")
    assert(hasIndoorTiles, "Other rooms should have indoor tiles")
    
    println(s"Starting room tiles: ${startRoomTiles.mkString(", ")}")
    println(s"Other room tiles: ${otherRoomTiles.mkString(", ")}")
  }
  
  test("Outdoor area encompasses entire dungeon with grass and tree perimeter") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 10, lockedDoorCount = 0, itemCount = 0)
    
    // Calculate dungeon bounds
    val minRoomX = dungeon.roomGrid.map(_.x).min
    val maxRoomX = dungeon.roomGrid.map(_.x).max
    val minRoomY = dungeon.roomGrid.map(_.y).min
    val maxRoomY = dungeon.roomGrid.map(_.y).max
    
    // With 2 room-widths of padding, outdoor area should extend beyond dungeon bounds
    val outdoorPadding = 2
    val expectedOutdoorMinX = (minRoomX - outdoorPadding) * Dungeon.roomSize
    val expectedOutdoorMaxX = (maxRoomX + outdoorPadding + 1) * Dungeon.roomSize
    val expectedOutdoorMinY = (minRoomY - outdoorPadding) * Dungeon.roomSize
    val expectedOutdoorMaxY = (maxRoomY + outdoorPadding + 1) * Dungeon.roomSize
    
    // Check that grass tiles exist in the outdoor area (outside dungeon rooms)
    val outdoorGrassTiles = dungeon.tiles.filter { case (point, tileType) =>
      val roomPoint = Point(point.x / Dungeon.roomSize, point.y / Dungeon.roomSize)
      !dungeon.roomGrid.contains(roomPoint) && 
      (tileType == TileType.Grass1 || tileType == TileType.Grass2 || tileType == TileType.Grass3)
    }
    
    // Check that tree perimeter exists at outer bounds
    val outerPerimeterTrees = dungeon.tiles.filter { case (point, tileType) =>
      tileType == TileType.Tree &&
      (point.x == expectedOutdoorMinX || point.x == expectedOutdoorMaxX ||
       point.y == expectedOutdoorMinY || point.y == expectedOutdoorMaxY)
    }
    
    assert(outdoorGrassTiles.nonEmpty, "Outdoor area should have grass tiles surrounding dungeon")
    assert(outerPerimeterTrees.nonEmpty, "Outdoor area should have tree perimeter at outer bounds")
    
    println(s"Dungeon bounds: (${minRoomX * Dungeon.roomSize}, ${minRoomY * Dungeon.roomSize}) to (${maxRoomX * Dungeon.roomSize}, ${maxRoomY * Dungeon.roomSize})")
    println(s"Outdoor bounds: ($expectedOutdoorMinX, $expectedOutdoorMinY) to ($expectedOutdoorMaxX, $expectedOutdoorMaxY)")
    println(s"Found ${outdoorGrassTiles.size} grass tiles in outdoor area")
    println(s"Found ${outerPerimeterTrees.size} trees in outer perimeter")
  }
}
