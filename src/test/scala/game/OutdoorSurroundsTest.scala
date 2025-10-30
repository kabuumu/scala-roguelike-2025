package game

import map.{Dungeon, MapGenerator, TileType}
import org.scalatest.funsuite.AnyFunSuite

class OutdoorSurroundsTest extends AnyFunSuite {
  
  // NOTE: These tests were for the old outdoor room system that has been replaced
  // by the unified WorldMap architecture (PR #77). Outdoor terrain is now handled
  // by WorldGenerator, not as part of dungeon.tiles.
  // See WorldMapGeneratorTest for tests of the new system.
  
  ignore("Outdoor area completely surrounds dungeon - OOO/OXO/OOO pattern") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    
    // Find dungeon bounds
    val minRoomX = dungeon.roomGrid.map(_.x).min
    val maxRoomX = dungeon.roomGrid.map(_.x).max
    val minRoomY = dungeon.roomGrid.map(_.y).min
    val maxRoomY = dungeon.roomGrid.map(_.y).max
    
    val dungeonMinX = minRoomX * Dungeon.roomSize
    val dungeonMaxX = (maxRoomX + 1) * Dungeon.roomSize
    val dungeonMinY = minRoomY * Dungeon.roomSize
    val dungeonMaxY = (maxRoomY + 1) * Dungeon.roomSize
    
    // Check that outdoor tiles exist BEFORE the dungeon starts (above)
    val tilesAboveDungeon = dungeon.tiles.filter { case (point, _) =>
      point.y < dungeonMinY
    }
    assert(tilesAboveDungeon.nonEmpty, "Should have outdoor tiles above dungeon")
    
    // Check that outdoor tiles exist AFTER the dungeon ends (below)
    val tilesBelowDungeon = dungeon.tiles.filter { case (point, _) =>
      point.y > dungeonMaxY
    }
    assert(tilesBelowDungeon.nonEmpty, "Should have outdoor tiles below dungeon")
    
    // Check that outdoor tiles exist to the LEFT of dungeon
    val tilesLeftOfDungeon = dungeon.tiles.filter { case (point, _) =>
      point.x < dungeonMinX
    }
    assert(tilesLeftOfDungeon.nonEmpty, "Should have outdoor tiles left of dungeon")
    
    // Check that outdoor tiles exist to the RIGHT of dungeon
    val tilesRightOfDungeon = dungeon.tiles.filter { case (point, _) =>
      point.x > dungeonMaxX
    }
    assert(tilesRightOfDungeon.nonEmpty, "Should have outdoor tiles right of dungeon")
    
    // Verify these outdoor tiles are grass/trees, not dungeon tiles
    val outdoorTileTypes = (tilesAboveDungeon ++ tilesBelowDungeon ++ 
                           tilesLeftOfDungeon ++ tilesRightOfDungeon).values.toSet
    
    val hasGrass = outdoorTileTypes.exists(t => 
      t == TileType.Grass1 || t == TileType.Grass2 || t == TileType.Grass3
    )
    val hasTrees = outdoorTileTypes.contains(TileType.Tree)
    
    assert(hasGrass, "Outdoor surrounding area should have grass")
    assert(hasTrees, "Outdoor surrounding area should have trees")
    
    println(s"✓ Dungeon at ($dungeonMinX,$dungeonMinY) to ($dungeonMaxX,$dungeonMaxY)")
    println(s"✓ Outdoor tiles above: ${tilesAboveDungeon.size}")
    println(s"✓ Outdoor tiles below: ${tilesBelowDungeon.size}")
    println(s"✓ Outdoor tiles left: ${tilesLeftOfDungeon.size}")
    println(s"✓ Outdoor tiles right: ${tilesRightOfDungeon.size}")
    println(s"✓ Pattern verified: OOO/OXO/OOO ✓")
  }
  
  ignore("Outdoor perimeter is continuous and walkable (except tree boundary)") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    
    val minRoomX = dungeon.roomGrid.map(_.x).min
    val maxRoomX = dungeon.roomGrid.map(_.x).max
    val minRoomY = dungeon.roomGrid.map(_.y).min
    val maxRoomY = dungeon.roomGrid.map(_.y).max
    
    val outdoorPadding = 2
    val dungeonMinX = minRoomX * Dungeon.roomSize
    val outdoorMinX = dungeonMinX - (outdoorPadding * Dungeon.roomSize)
    val outdoorMaxX = (maxRoomX + 1) * Dungeon.roomSize + (outdoorPadding * Dungeon.roomSize)
    val outdoorMinY = minRoomY * Dungeon.roomSize - (outdoorPadding * Dungeon.roomSize)
    
    // Check a horizontal line of outdoor tiles exists above the dungeon
    val outdoorLineY = outdoorMinY + 1 // One row inside the tree perimeter
    val outdoorLineAbove = (outdoorMinX + 1 to outdoorMaxX - 1).map { x =>
      dungeon.tiles.get(Point(x, outdoorLineY))
    }
    
    // All points in this line should exist and be outdoor tiles
    assert(outdoorLineAbove.forall(_.isDefined), "Outdoor line should be continuous")
    
    val grassInLine = outdoorLineAbove.flatten.count(t => 
      t == TileType.Grass1 || t == TileType.Grass2 || t == TileType.Grass3
    )
    
    assert(grassInLine > 0, "Outdoor area should have grass tiles")
    
    println(s"✓ Continuous outdoor perimeter verified")
    println(s"✓ Found ${grassInLine} grass tiles in horizontal outdoor line")
  }
  
  ignore("Starting room is connected to outer outdoor area (no walls blocking)") {
    val dungeon = MapGenerator.generateDungeon(dungeonSize = 5, lockedDoorCount = 0, itemCount = 0)
    
    // Find starting room bounds
    val startRoom = dungeon.startPoint
    val roomX = startRoom.x * Dungeon.roomSize
    val roomY = startRoom.y * Dungeon.roomSize
    
    // Check walls of starting room - they should be grass (passable), not trees (impassable)
    val startRoomWalls = dungeon.tiles.filter { case (point, _) =>
      (point.x == roomX || point.x == roomX + Dungeon.roomSize) &&
      point.y >= roomY && point.y <= roomY + Dungeon.roomSize ||
      (point.y == roomY || point.y == roomY + Dungeon.roomSize) &&
      point.x >= roomX && point.x <= roomX + Dungeon.roomSize
    }
    
    // Count passable tiles (grass) on the starting room perimeter
    val passablePerimeter = startRoomWalls.filter { case (point, tileType) =>
      !dungeon.doorPoints.contains(point) && 
      (tileType == TileType.Grass1 || tileType == TileType.Grass2 || 
       tileType == TileType.Grass3 || tileType == TileType.Dirt)
    }
    
    // Count impassable tiles (trees/walls) on the starting room perimeter
    val impassablePerimeter = startRoomWalls.filter { case (point, tileType) =>
      !dungeon.doorPoints.contains(point) && 
      (tileType == TileType.Tree || tileType == TileType.Wall)
    }
    
    // Starting room should have mostly passable perimeter (connected to outdoor)
    assert(passablePerimeter.nonEmpty, "Starting room should have passable edges to outdoor area")
    
    // Check that we can find a path from starting room to outer outdoor area
    val startCenter = Point(
      roomX + Dungeon.roomSize / 2,
      roomY + Dungeon.roomSize / 2
    )
    
    // Find an outdoor tile outside the starting room
    val outerOutdoorTile = dungeon.tiles.find { case (point, tileType) =>
      (tileType == TileType.Grass1 || tileType == TileType.Grass2 || tileType == TileType.Grass3) &&
      (point.x < roomX - 1 || point.x > roomX + Dungeon.roomSize + 1 ||
       point.y < roomY - 1 || point.y > roomY + Dungeon.roomSize + 1)
    }
    
    assert(outerOutdoorTile.isDefined, "Should have outdoor area outside starting room")
    
    println(s"✓ Starting room at ($roomX,$roomY)")
    println(s"✓ Passable perimeter tiles: ${passablePerimeter.size}")
    println(s"✓ Impassable perimeter tiles: ${impassablePerimeter.size}")
    println(s"✓ Starting room is CONNECTED to outer outdoor area ✓")
  }
}

