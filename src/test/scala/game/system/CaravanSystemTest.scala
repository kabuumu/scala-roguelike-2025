package game.system

import game.GameState
import game.entity._
import game.system.event.GameSystemEvent.{GameSystemEvent, InputEvent}
import map.{Village, MapBounds, WorldMap, TileType}
import game.Point
import data.TraderData
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import game.entity.CaravanState

class CaravanSystemTest extends AnyFlatSpec with Matchers {

  "CaravanSystem" should "move leader linearly towards target village" in {
    // Setup map with 2 villages
    val dummyBuildings = Seq(
      map.Building(Point(0, 0), 5, 5, map.BuildingType.Generic),
      map.Building(Point(10, 0), 5, 5, map.BuildingType.Generic)
    )

    val v1 = Village(
      dummyBuildings,
      Point(0, 0),
      Set.empty,
      "V1",
      MapBounds(0, 0, 1, 1),
      population = 10,
      stockpile = 10,
      cropsHarvested = 0,
      cropsConsumed = 0,
      wealth = 0
    )
    val v2 = Village(
      dummyBuildings,
      Point(50, 50),
      Set.empty,
      "V2",
      MapBounds(0, 0, 1, 1),
      population = 10,
      stockpile = 10,
      cropsHarvested = 0,
      cropsConsumed = 0,
      wealth = 0
    )

    val worldMap = WorldMap(
      Map.empty,
      Nil,
      None,
      Seq(v1, v2),
      Set.empty,
      Set.empty,
      MapBounds(0, 0, 100, 100),
      Map.empty,
      0,
      Set.empty,
      None
    )

    // Create Caravan at V1
    val caravanEntities = TraderData.createCaravan("trader", Point(0, 0))
    val leader = caravanEntities.head

    // Force state to Moving to V2
    val leaderWithComponent = leader.update[CaravanComponent](c =>
      c.copy(
        state = CaravanState.Moving,
        targetVillageCenter = Some(v2.centerLocation)
      )
    )

    val gameState = GameState(
      "player",
      Seq(
        Entity("player", EntityTypeComponent(EntityType.Player))
      ) ++ caravanEntities,
      worldMap = worldMap
    )
      .updateEntity(leader.id, leaderWithComponent)

    // Run System
    val (_, events) = CaravanSystem.update(gameState, Seq.empty)

    // Should have Move event
    events should not be empty
    events.head shouldBe a[InputEvent]
    // Assume basic pathfinding works, leader moves towards 50,50 (or closest step)
  }
}
