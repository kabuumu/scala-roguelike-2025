package game

import org.scalatest.funsuite.AnyFunSuite
import game.entity.Trader._
import game.entity.Movement._

class VillageTraderTest extends AnyFunSuite {
  
  test("Village buildings have traders except player's starting building") {
    // Get the starting game state
    val gameState = StartingState.startingGameState
    
    // Find all trader entities in villages
    val villageTraders = gameState.entities.filter { entity =>
      (entity.isTrader || entity.has[game.entity.NameComponent]) && entity.id.startsWith("npc-village")
    }
    
    // There should be traders in village buildings
    assert(villageTraders.nonEmpty, "Villages should have traders")
    
    println(s"Found ${villageTraders.size} village traders")
    
    // Get the player spawn point
    val playerSpawnPoint = Point(0, 0)
    
    // Count buildings with and without traders
    var buildingsWithPlayerSpawn = 0
    var buildingsWithTraders = 0
    
    // Check that all village buildings have traders except the one containing player spawn
    gameState.worldMap.villages.zipWithIndex.foreach { case (village, villageIdx) =>
      println(s"\nVillage $villageIdx has ${village.buildings.length} buildings:")
      village.buildings.zipWithIndex.foreach { case (building, buildingIdx) =>
        val (minBounds, maxBounds) = building.bounds
        val containsPlayerSpawn = 
          playerSpawnPoint.x >= minBounds.x && playerSpawnPoint.x <= maxBounds.x &&
          playerSpawnPoint.y >= minBounds.y && playerSpawnPoint.y <= maxBounds.y
        
        val traderId = s"trader-village-$villageIdx-building-$buildingIdx"
        val hasTrader = villageTraders.exists(_.id == traderId)
        
        if (containsPlayerSpawn) {
          buildingsWithPlayerSpawn += 1
          assert(!hasTrader, s"Building at ${building.location} contains player spawn, should NOT have trader")
          println(s"  Building $buildingIdx: Contains player spawn - NO trader (correct)")
        } else {
          assert(hasTrader, s"Building at ${building.location} should have trader $traderId")
          buildingsWithTraders += 1
          println(s"  Building $buildingIdx: Has trader (correct)")
        }
      }
    }
    
    println(s"\nTotal buildings with player spawn: $buildingsWithPlayerSpawn")
    println(s"Total buildings with traders: $buildingsWithTraders")
    
    // Either:
    // - At least one building contains the player spawn and has no trader
    // - All buildings have traders (if player spawns in open space)
    if (buildingsWithPlayerSpawn > 0) {
      println(s"Player spawns inside a building - that building has no trader")
    } else {
      println(s"Player spawns in open space - all buildings have traders")
    }
    
    // Total buildings should equal traders + buildings with player spawn
    val totalBuildings = gameState.worldMap.villages.map(_.buildings.length).sum
    assert(totalBuildings == buildingsWithTraders + buildingsWithPlayerSpawn, 
      "All buildings should either have traders or contain player spawn")
  }
  
  test("Traders are positioned at building centers") {
    val gameState = StartingState.startingGameState
    
    val villageTraders = gameState.entities.filter { entity =>
      entity.isTrader && entity.id.startsWith("trader-village")
    }
    
    villageTraders.foreach { trader =>
      // Extract village and building indices from trader ID
      val pattern = """trader-village-(\d+)-building-(\d+)""".r
      trader.id match {
        case pattern(villageIdxStr, buildingIdxStr) =>
          val villageIdx = villageIdxStr.toInt
          val buildingIdx = buildingIdxStr.toInt
          
          val village = gameState.worldMap.villages(villageIdx)
          val building = village.buildings(buildingIdx)
          val expectedCenter = building.centerTile
          
          val traderPos = trader.position
          assert(traderPos == expectedCenter, 
            s"Trader ${trader.id} at $traderPos should be at building center $expectedCenter")
        case _ =>
          fail(s"Trader ID ${trader.id} doesn't match expected pattern")
      }
    }
    
    println(s"All ${villageTraders.size} traders are correctly positioned at building centers")
  }
  
  test("All traders have the default trader inventory") {
    val gameState = StartingState.startingGameState
    
    val allTraders = gameState.entities.filter(_.isTrader)
    
    assert(allTraders.nonEmpty, "Game should have traders")
    
    allTraders.foreach { trader =>
      assert(trader.trader.isDefined, s"Trader ${trader.id} should have Trader component")
      
      val traderComponent = trader.trader.get
      assert(traderComponent.tradeInventory.nonEmpty, 
        s"Trader ${trader.id} should have items in inventory")
      
      println(s"Trader ${trader.id} has ${traderComponent.tradeInventory.size} items")
    }
  }
}
