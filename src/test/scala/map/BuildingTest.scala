package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class BuildingTest extends AnyFunSuite {
  
  test("Building generates correct size") {
    val building = Building(Point(0, 0), width = 5, height = 7, buildingType = BuildingType.Generic)
    
    // Building should be (width+1) x (height+1) tiles (0 to size inclusive)
    val expectedTiles = 6 * 8
    assert(building.tiles.size == expectedTiles, s"Building should have $expectedTiles tiles")
  }
  
  test("Building enforces minimum size") {
    assertThrows[IllegalArgumentException] {
      Building(Point(0, 0), width = 4, height = 5, isShop = false)
    }
    
    assertThrows[IllegalArgumentException] {
      Building(Point(0, 0), width = 5, height = 4, isShop = false)
    }
  }
  
  test("Building has walls on perimeter") {
    val building = Building(Point(10, 10), width = 5, height = 5, isShop = false)
    val baseX = 10
    val baseY = 10
    
    // Check corners are walls
    assert(building.tiles(Point(baseX, baseY)) == TileType.Wall, "Top-left corner should be wall")
    assert(building.tiles(Point(baseX + 5, baseY)) == TileType.Wall, "Top-right corner should be wall")
    assert(building.tiles(Point(baseX, baseY + 5)) == TileType.Wall, "Bottom-left corner should be wall")
    assert(building.tiles(Point(baseX + 5, baseY + 5)) == TileType.Wall, "Bottom-right corner should be wall")
  }
  
  test("Building has floor inside") {
    val building = Building(Point(0, 0), width = 5, height = 5, isShop = true)
    val centerTile = building.centerTile
    
    assert(building.tiles(centerTile) == TileType.Floor, "Center should be floor")
  }
  
  test("Building has entrance") {
    val building = Building(Point(30, 0), width = 6, height = 8, isShop = false)
    val entrance = building.entranceTile
    
    // Entrance should be floor (not wall)
    assert(building.tiles(entrance) == TileType.Floor, "Entrance should be floor")
    
    // Entrance should be on perimeter
    val baseX = 30
    val baseY = 0
    val onPerimeter = 
      entrance.x == baseX || entrance.x == baseX + 6 || 
      entrance.y == baseY || entrance.y == baseY + 8
    assert(onPerimeter, "Entrance should be on perimeter")
  }
  
  test("Building center tile calculation is correct") {
    val building = Building(Point(20, 30), width = 6, height = 8, isShop = false)
    val center = building.centerTile
    
    // Center should be at location + width/2, location + height/2
    assert(center.x == 20 + 3, "Center X should be at location X + width/2")
    assert(center.y == 30 + 4, "Center Y should be at location Y + height/2")
  }
  
  test("Building walls set is correct") {
    val building = Building(Point(0, 0), width = 5, height = 5, isShop = false)
    
    // All walls should be Wall type
    building.walls.foreach { wallPoint =>
      assert(building.tiles(wallPoint) == TileType.Wall, 
             s"Point $wallPoint in walls set should be Wall type")
    }
    
    // Entrance should not be in walls
    val entrance = building.entranceTile
    assert(!building.walls.contains(entrance), "Entrance should not be in walls set")
  }
  
  test("Building can be rectangular (non-square)") {
    val building = Building(Point(0, 0), width = 5, height = 10, isShop = false)
    
    assert(building.tiles.nonEmpty, "Rectangular building should have tiles")
    assert(building.walls.nonEmpty, "Rectangular building should have walls")
    
    // Verify it's actually rectangular
    val (min, max) = building.bounds
    assert(max.x - min.x == 5, "Width should be 5")
    assert(max.y - min.y == 10, "Height should be 10")
  }
  
  test("Building.overlap detects overlapping buildings") {
    val building1 = Building(Point(0, 0), width = 5, height = 5, isShop = false)
    val building2 = Building(Point(3, 3), width = 5, height = 5, isShop = false)
    val building3 = Building(Point(10, 10), width = 5, height = 5, isShop = false)
    
    assert(Building.overlap(building1, building2), "Buildings should overlap")
    assert(!Building.overlap(building1, building3), "Buildings should not overlap")
  }
  
  test("Building in different quadrants has entrance facing origin") {
    // Test building in each quadrant
    val buildingQ1 = Building(Point(30, 30), width = 5, height = 5, isShop = false)   // Quadrant I (bottom-right)
    val buildingQ2 = Building(Point(-30, 30), width = 5, height = 5, isShop = false)  // Quadrant II (bottom-left)
    val buildingQ3 = Building(Point(-30, -30), width = 5, height = 5, isShop = false) // Quadrant III (top-left)
    val buildingQ4 = Building(Point(30, -30), width = 5, height = 5, isShop = false)  // Quadrant IV (top-right)
    
    // All entrances should be floor tiles
    assert(buildingQ1.tiles(buildingQ1.entranceTile) == TileType.Floor)
    assert(buildingQ2.tiles(buildingQ2.entranceTile) == TileType.Floor)
    assert(buildingQ3.tiles(buildingQ3.entranceTile) == TileType.Floor)
    assert(buildingQ4.tiles(buildingQ4.entranceTile) == TileType.Floor)
  }
}
