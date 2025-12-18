package game

import org.scalatest.funsuite.AnyFunSuite
import game.entity.Trader._
import game.entity.Movement._

class VillageTraderTest extends AnyFunSuite {

  test("Village buildings have NPCs except player's starting building") {
    val gameState = StartingState.startingGameState

    val villageNPCs = gameState.entities.filter { entity =>
      (entity.isTrader || entity.has[game.entity.NameComponent]) && entity.id
        .startsWith("npc-village")
    }

    assert(villageNPCs.nonEmpty, "Villages should have NPCs")
    println(s"Found ${villageNPCs.size} village NPCs")

    val playerSpawnPoint = Point(0, 0)
    var buildingsWithPlayerSpawn = 0
    var buildingsWithNPCs = 0

    gameState.worldMap.villages.zipWithIndex.foreach {
      case (village, villageIdx) =>
        println(
          s"\nVillage $villageIdx has ${village.buildings.length} buildings:"
        )
        village.buildings.zipWithIndex.foreach { case (building, buildingIdx) =>
          val (minBounds, maxBounds) = building.bounds
          val containsPlayerSpawn =
            playerSpawnPoint.x >= minBounds.x && playerSpawnPoint.x <= maxBounds.x &&
              playerSpawnPoint.y >= minBounds.y && playerSpawnPoint.y <= maxBounds.y

          val npcId = s"npc-village-$villageIdx-building-$buildingIdx"
          val hasNPC = villageNPCs.exists(_.id == npcId)

          if (containsPlayerSpawn) {
            buildingsWithPlayerSpawn += 1
            assert(
              !hasNPC,
              s"Building at ${building.location} contains player spawn, should NOT have NPC"
            )
            println(
              s"  Building $buildingIdx: Contains player spawn - NO NPC (correct)"
            )
          } else {
            assert(
              hasNPC,
              s"Building at ${building.location} should have NPC $npcId"
            )
            buildingsWithNPCs += 1
            println(s"  Building $buildingIdx: Has NPC (correct)")
          }
        }
    }

    println(s"\nTotal buildings with player spawn: $buildingsWithPlayerSpawn")
    println(s"Total buildings with NPCs: $buildingsWithNPCs")

    if (buildingsWithPlayerSpawn > 0) {
      println(s"Player spawns inside a building - that building has no NPC")
    } else {
      println(s"Player spawns in open space - all buildings have NPCs")
    }

    val totalBuildings = gameState.worldMap.villages.map(_.buildings.length).sum
    assert(
      totalBuildings == buildingsWithNPCs + buildingsWithPlayerSpawn,
      "All buildings should either have NPCs or contain player spawn"
    )
  }

  test("NPCs are positioned at building centers") {
    val gameState = StartingState.startingGameState

    val villageNPCs = gameState.entities.filter { entity =>
      (entity.isTrader || entity.has[game.entity.NameComponent]) && entity.id
        .startsWith("npc-village")
    }

    villageNPCs.foreach { npc =>
      val pattern = """npc-village-(\d+)-building-(\d+)""".r
      npc.id match {
        case pattern(villageIdxStr, buildingIdxStr) =>
          val villageIdx = villageIdxStr.toInt
          val buildingIdx = buildingIdxStr.toInt

          val village = gameState.worldMap.villages(villageIdx)
          val building = village.buildings(buildingIdx)
          val expectedCenter = building.centerTile

          val npcPos = npc.position
          assert(
            npcPos == expectedCenter,
            s"NPC ${npc.id} at $npcPos should be at building center $expectedCenter"
          )
        case _ =>
          fail(s"NPC ID ${npc.id} doesn't match expected pattern")
      }
    }

    println(
      s"All ${villageNPCs.size} NPCs are correctly positioned at building centers"
    )
  }

  test("NPCs have appropriate inventories") {
    val gameState = StartingState.startingGameState

    val traders = gameState.entities.filter(_.isTrader)
    assert(traders.nonEmpty, "Game should have some traders")

    traders.foreach { trader =>
      val traderComponent = trader.trader.get
      assert(
        traderComponent.tradeInventory.nonEmpty,
        s"Trader ${trader.id} should have items in inventory"
      )
    }

    val villagers = gameState.entities.filter(e =>
      e.id.startsWith("npc-village") && !e.isTrader
    )
    villagers.foreach { villager =>
      assert(
        villager.has[game.entity.Conversation],
        s"Villager ${villager.id} should have Conversation component"
      )
    }
  }
}
