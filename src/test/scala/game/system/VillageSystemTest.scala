package game.system

import game.GameState
import game.entity._
import game.system.event.GameSystemEvent.GameSystemEvent
import map.{Village, MapBounds, WorldMap, TileType, Building, BuildingType}
import game.Point
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VillageSystemTest extends AnyFlatSpec with Matchers {

  // Minimal dummy world map
  val dummyWorldMap = WorldMap(
    tiles = Map.empty,
    dungeons = Nil,
    shop = None,
    villages = Nil,
    paths = Set.empty,
    bridges = Set.empty,
    bounds = MapBounds(0, 0, 100, 100),
    chunks = Map.empty,
    seed = 123L,
    processedRegions = Set.empty,
    lastCenterChunk = None
  )

  // Dummy buildings for Village requirement (min 2)
  val dummyBuildings = Seq(
    Building(Point(10, 10), 5, 5, BuildingType.Generic),
    Building(Point(20, 10), 5, 5, BuildingType.Generic)
  )

  "VillageSystem" should "consume food from stockpile over time" in {
    // 1. Setup Village with stockpile and population
    val village = Village(
      buildings = dummyBuildings,
      centerLocation = Point(10, 10),
      paths = Set.empty,
      name = "Test Village",
      bounds = MapBounds(0, 0, 20, 20),
      population = 100, // High population ensures consumption in 600 ticks
      stockpile = 10,
      cropsHarvested = 0,
      cropsConsumed = 0
    )

    val worldMap = dummyWorldMap.copy(villages = Seq(village))
    val gameState = GameState(
      "player",
      Seq(Entity("player", EntityTypeComponent(EntityType.Player))),
      worldMap = worldMap
    )

    // 2. Run System multiple times
    var currentState = gameState
    for (_ <- 1 to 600) {
      val (nextState, _) = VillageSystem.update(currentState, Seq.empty)
      currentState = nextState
    }

    // 3. Verify Consumption
    val updatedVillage = currentState.worldMap.villages.head
    updatedVillage.stockpile should be < 10
    updatedVillage.cropsConsumed should be > 0
  }

  it should "not consume below zero stockpile" in {
    // 1. Setup Village with 0 stockpile
    val village = Village(
      buildings = dummyBuildings,
      centerLocation = Point(10, 10),
      paths = Set.empty,
      name = "Test Village",
      bounds = MapBounds(0, 0, 20, 20),
      population = 1000,
      stockpile = 0,
      cropsHarvested = 0,
      cropsConsumed = 0
    )

    val worldMap = dummyWorldMap.copy(villages = Seq(village))
    val gameState = GameState(
      "player",
      Seq(Entity("player", EntityTypeComponent(EntityType.Player))),
      worldMap = worldMap
    )

    // 2. Run System
    var currentState = gameState
    for (_ <- 1 to 100) {
      val (nextState, _) = VillageSystem.update(currentState, Seq.empty)
      currentState = nextState
    }

    // 3. Verify Stockpile stays at 0
    val updatedVillage = currentState.worldMap.villages.head
    updatedVillage.stockpile should be(0)
  }
}
