package game

import org.scalatest.funsuite.AnyFunSuite
import map._
import game.entity.Movement.position

class ConversationAndPathFixTest extends AnyFunSuite {

  test("Path tiles (dirt and bridge) persist across chunk updates") {
    val seed = 100L
    val state = StartingState.startAdventure(seed)
    val worldMap = state.worldMap

    assert(worldMap.paths.nonEmpty, "World map must have path tiles generated")

    // Force chunk manager update around a different chunk position
    val updatedMap = ChunkManager.updateChunks(
      Point(state.playerEntity.position.x + 50, state.playerEntity.position.y + 50),
      worldMap,
      WorldConfig(bounds = worldMap.bounds, seed = seed),
      seed
    )

    // Verify all path tiles remain walkable Dirt or Bridge tiles in updatedMap.tiles
    val missingPaths = worldMap.paths.filterNot { p =>
      updatedMap.getTile(p).contains(TileType.Dirt) || updatedMap.getTile(p).contains(TileType.Bridge)
    }

    assert(
      missingPaths.isEmpty,
      s"All ${worldMap.paths.size} path tiles must persist as Dirt/Bridge after chunk updates. Missing count: ${missingPaths.size}"
    )
  }

  test("Dynamic direction calculation correctly identifies relative compass directions") {
    val origin = Point(100, 100)

    assert(StartingState.getRelativeDirection(origin, Point(160, 100)) == "to the east")
    assert(StartingState.getRelativeDirection(origin, Point(40, 100)) == "to the west")
    assert(StartingState.getRelativeDirection(origin, Point(100, 160)) == "to the south")
    assert(StartingState.getRelativeDirection(origin, Point(100, 40)) == "to the north")
  }

  test("StartingState sets dynamic quest and elder dialogue direction based on actual dungeon placement") {
    Seq(42L, 100L, 2026L).foreach { seed =>
      val state = StartingState.startAdventure(seed)
      val worldMap = state.worldMap
      val dungeon = worldMap.dungeons.head
      val dungeonEntrance = Dungeon.getApproachTile(dungeon.startPoint, dungeon.entranceSide)

      val expectedDir = StartingState.getRelativeDirection(state.playerEntity.position, dungeonEntrance)

      val elderConv = state.entities
        .find(_.get[game.entity.NameComponent].exists(_.name == "Elder"))
        .flatMap(_.get[game.entity.Conversation])
        .get

      assert(
        elderConv.text.contains(expectedDir),
        s"Seed $seed: Elder conversation '${elderConv.text}' should contain dynamic direction '$expectedDir'"
      )
    }
  }
}
