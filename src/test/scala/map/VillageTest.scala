package map

import game.Point
import org.scalatest.funsuite.AnyFunSuite

class VillageTest extends AnyFunSuite {
  
  test("Village generates correct number of buildings") {
    val village = Village.generateVillage(Point(0, 0), seed = 12345)
    
    assert(village.buildings.length >= 3 && village.buildings.length <= 5, 
           s"Village should have 3-5 buildings, but has ${village.buildings.length}")
  }
  
  test("Village has at least one shop type building") {
    val village = Village.generateVillage(Point(0, 0), seed = 67890)
    
    val shopCount = village.buildings.count(_.isShop)
    assert(shopCount >= 1, s"Village should have at least 1 shop/healer, but has $shopCount")
  }

  test("Village has varied building types") {
    val village = Village.generateVillage(Point(0, 0), seed = 12345)

    val types = village.buildings.map(_.buildingType).toSet
    // Should have at least Generic and one other type in a typical village of 3-5 buildings
    assert(types.size >= 2, s"Village should have variety in building types, but found only $types")
  }
  
  test("Village buildings meet minimum size requirements") {
    val village = Village.generateVillage(Point(0, 0), seed = 11111)
    
    village.buildings.foreach { building =>
      assert(building.width >= 5, s"Building width should be at least 5, but is ${building.width}")
      assert(building.height >= 5, s"Building height should be at least 5, but is ${building.height}")
    }
  }
  
  test("Village tiles include all building tiles") {
    val village = Village.generateVillage(Point(0, 0), seed = 22222)
    
    village.buildings.foreach { building =>
      building.tiles.foreach { case (point, tileType) =>
        assert(village.tiles.contains(point), s"Village should contain building tile at $point")
      }
    }
  }
  
  test("Village walls include all building walls") {
    val village = Village.generateVillage(Point(0, 0), seed = 33333)
    
    village.buildings.foreach { building =>
      building.walls.foreach { wallPoint =>
        assert(village.walls.contains(wallPoint), s"Village should contain building wall at $wallPoint")
      }
    }
  }
  
  test("Village.shopBuilding returns the shop building") {
    val village = Village.generateVillage(Point(0, 0), seed = 44444)
    
    assert(village.shopBuilding.isShop, "Shop building should have isShop = true")
  }
  
  test("Village.entrances returns all building entrances") {
    val village = Village.generateVillage(Point(0, 0), seed = 55555)
    
    assert(village.entrances.length == village.buildings.length, 
           "Village should have one entrance per building")
  }
  
  test("Village.findVillageLocation avoids dungeons") {
    val dungeonBounds = Seq(MapBounds(-5, 5, -5, 5))
    val worldBounds = MapBounds(-10, 10, -10, 10)
    
    val villageLocation = Village.findVillageLocation(dungeonBounds, worldBounds, preferredDistance = 30)
    
    // Village should not be too close to dungeon center
    val dungeonCenter = Point(0, 0)
    val distance = math.sqrt(
      math.pow(villageLocation.x - dungeonCenter.x, 2) + 
      math.pow(villageLocation.y - dungeonCenter.y, 2)
    )
    
    // Should be at least somewhat distant (accounting for dungeon bounds + padding)
    assert(distance > 20, s"Village at $villageLocation should be far from dungeon center, but distance is $distance")
  }
  
  test("Village.findVillageLocation is within world bounds") {
    val dungeonBounds = Seq(MapBounds(1, 5, 1, 5))
    val worldBounds = MapBounds(-10, 10, -10, 10)
    
    val villageLocation = Village.findVillageLocation(dungeonBounds, worldBounds, preferredDistance = 30)
    
    val (worldMinX, worldMaxX, worldMinY, worldMaxY) = worldBounds.toTileBounds(10)
    val margin = 40  // Village size
    
    assert(villageLocation.x >= worldMinX + margin && villageLocation.x <= worldMaxX - margin,
           s"Village X at ${villageLocation.x} should be within world bounds")
    assert(villageLocation.y >= worldMinY + margin && villageLocation.y <= worldMaxY - margin,
           s"Village Y at ${villageLocation.y} should be within world bounds")
  }
  
  test("Village generation is deterministic with same seed") {
    val village1 = Village.generateVillage(Point(100, 100), seed = 99999)
    val village2 = Village.generateVillage(Point(100, 100), seed = 99999)
    
    assert(village1.buildings.length == village2.buildings.length, 
           "Villages with same seed should have same number of buildings")
    
    village1.buildings.zip(village2.buildings).foreach { case (b1, b2) =>
      assert(b1.width == b2.width, "Building widths should match")
      assert(b1.height == b2.height, "Building heights should match")
      assert(b1.isShop == b2.isShop, "Shop status should match")
    }
  }
  
  test("Village with different seeds generates different layouts") {
    val village1 = Village.generateVillage(Point(0, 0), seed = 11111)
    val village2 = Village.generateVillage(Point(0, 0), seed = 22222)
    
    // At least some aspect should differ (number of buildings or building sizes)
    val different = 
      village1.buildings.length != village2.buildings.length ||
      village1.buildings.zip(village2.buildings).exists { case (b1, b2) =>
        b1.width != b2.width || b1.height != b2.height
      }
    
    assert(different, "Villages with different seeds should generate different layouts")
  }
  
  test("Village buildings do not overlap") {
    val village = Village.generateVillage(Point(0, 0), seed = 77777)
    
    // Check all pairs of buildings for overlap
    for {
      i <- village.buildings.indices
      j <- village.buildings.indices
      if i < j
    } {
      val overlap = Building.overlap(village.buildings(i), village.buildings(j))
      assert(!overlap, s"Buildings ${i} and ${j} should not overlap")
    }
  }
}
