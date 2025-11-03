package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class ShopTest extends AnyFunSuite {
  
  test("Shop generates correct size") {
    val shop = Shop(Point(2, 2), size = 10)
    
    // Shop should be 11x11 tiles (0 to size inclusive)
    val expectedTiles = 11 * 11
    assert(shop.tiles.size == expectedTiles, s"Shop should have $expectedTiles tiles")
  }
  
  test("Shop has walls on perimeter") {
    val shop = Shop(Point(0, 0), size = 10)
    val baseX = 0
    val baseY = 0
    
    // Check corners are walls
    assert(shop.tiles(Point(baseX, baseY)) == TileType.Wall, "Top-left corner should be wall")
    assert(shop.tiles(Point(baseX + 10, baseY)) == TileType.Wall, "Top-right corner should be wall")
    assert(shop.tiles(Point(baseX, baseY + 10)) == TileType.Wall, "Bottom-left corner should be wall")
    assert(shop.tiles(Point(baseX + 10, baseY + 10)) == TileType.Wall, "Bottom-right corner should be wall")
  }
  
  test("Shop has floor inside") {
    val shop = Shop(Point(0, 0), size = 10)
    val centerTile = shop.centerTile
    
    assert(shop.tiles(centerTile) == TileType.Floor, "Center should be floor")
  }
  
  test("Shop has entrance") {
    val shop = Shop(Point(3, 0), size = 10)
    val entrance = shop.entranceTile
    
    // Entrance should be floor (not wall)
    assert(shop.tiles(entrance) == TileType.Floor, "Entrance should be floor")
    
    // Entrance should be on perimeter
    val baseX = 3 * 10
    val baseY = 0
    val onPerimeter = 
      entrance.x == baseX || entrance.x == baseX + 10 || 
      entrance.y == baseY || entrance.y == baseY + 10
    assert(onPerimeter, "Entrance should be on perimeter")
  }
  
  test("Shop.findShopLocation avoids dungeon") {
    val dungeonBounds = MapBounds(-5, 5, -5, 5)
    val worldBounds = MapBounds(-10, 10, -10, 10)
    
    val shopLocation = Shop.findShopLocation(dungeonBounds, worldBounds, preferredDistance = 3)
    
    // Shop should not overlap dungeon (with 2 room padding)
    val tooClose = 
      shopLocation.x >= dungeonBounds.minRoomX - 2 && 
      shopLocation.x <= dungeonBounds.maxRoomX + 2 &&
      shopLocation.y >= dungeonBounds.minRoomY - 2 && 
      shopLocation.y <= dungeonBounds.maxRoomY + 2
    
    assert(!tooClose, s"Shop at $shopLocation should not be near dungeon bounds $dungeonBounds")
  }
  
  test("Shop.findShopLocation is within world bounds") {
    val dungeonBounds = MapBounds(1, 5, 1, 5)
    val worldBounds = MapBounds(-10, 10, -10, 10)
    
    val shopLocation = Shop.findShopLocation(dungeonBounds, worldBounds, preferredDistance = 3)
    
    assert(shopLocation.x >= worldBounds.minRoomX && shopLocation.x <= worldBounds.maxRoomX,
           s"Shop X at ${shopLocation.x} should be within world bounds")
    assert(shopLocation.y >= worldBounds.minRoomY && shopLocation.y <= worldBounds.maxRoomY,
           s"Shop Y at ${shopLocation.y} should be within world bounds")
  }
  
  test("Shop center tile calculation is correct") {
    val shop = Shop(Point(2, 3), size = 10)
    val center = shop.centerTile
    
    // Center should be at room * size + size / 2
    assert(center.x == 2 * 10 + 5, "Center X should be at room X * size + size/2")
    assert(center.y == 3 * 10 + 5, "Center Y should be at room Y * size + size/2")
  }
  
  test("Shop walls set is correct") {
    val shop = Shop(Point(0, 0), size = 10)
    
    // All walls should be Wall type
    shop.walls.foreach { wallPoint =>
      assert(shop.tiles(wallPoint) == TileType.Wall, 
             s"Point $wallPoint in walls set should be Wall type")
    }
    
    // Entrance should not be in walls
    val entrance = shop.entranceTile
    assert(!shop.walls.contains(entrance), "Entrance should not be in walls set")
  }
  
  test("Shop with different sizes works correctly") {
    val smallShop = Shop(Point(0, 0), size = 8)
    val largeShop = Shop(Point(0, 0), size = 12)
    
    assert(smallShop.tiles.size == 9 * 9, "Small shop should have correct tile count")
    assert(largeShop.tiles.size == 13 * 13, "Large shop should have correct tile count")
  }
  
  test("Shop in different quadrants has entrance facing origin") {
    // Test shop in each quadrant
    val shopQ1 = Shop(Point(3, 3), size = 10)   // Quadrant I (bottom-right)
    val shopQ2 = Shop(Point(-3, 3), size = 10)  // Quadrant II (bottom-left)
    val shopQ3 = Shop(Point(-3, -3), size = 10) // Quadrant III (top-left)
    val shopQ4 = Shop(Point(3, -3), size = 10)  // Quadrant IV (top-right)
    
    // All entrances should be floor tiles
    assert(shopQ1.tiles(shopQ1.entranceTile) == TileType.Floor)
    assert(shopQ2.tiles(shopQ2.entranceTile) == TileType.Floor)
    assert(shopQ3.tiles(shopQ3.entranceTile) == TileType.Floor)
    assert(shopQ4.tiles(shopQ4.entranceTile) == TileType.Floor)
    
    println(s"Q1 shop at ${shopQ1.location} has entrance at ${shopQ1.entranceTile}")
    println(s"Q2 shop at ${shopQ2.location} has entrance at ${shopQ2.entranceTile}")
    println(s"Q3 shop at ${shopQ3.location} has entrance at ${shopQ3.entranceTile}")
    println(s"Q4 shop at ${shopQ4.location} has entrance at ${shopQ4.entranceTile}")
  }
}
