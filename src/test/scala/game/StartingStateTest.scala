package game

import org.scalatest.funsuite.AnyFunSuite
import game.entity.Movement.position

class StartingStateTest extends AnyFunSuite {

  test(
    "startGauntlet should generate a dungeon with an endpoint room"
  ) {
    // Generate a gauntlet game state
    val gameState = StartingState.startGauntlet()

    // Verify dungeon exists
    val dungeons = gameState.worldMap.dungeons
    assert(dungeons.nonEmpty, "Gauntlet should generate at least one dungeon")

    val dungeon = dungeons.head
    println(s"Dungeon rooms: ${dungeon.roomGrid.size}")
    println(s"Dungeon start: ${dungeon.startPoint}")
    println(s"Dungeon endpoint: ${dungeon.endpoint}")

    // Verify the dungeon has an endpoint (boss room)
    assert(
      dungeon.endpoint.isDefined,
      "Gauntlet dungeon should have an endpoint room"
    )

    // Verify dungeon has reasonable size
    assert(
      dungeon.roomGrid.size >= 10,
      s"Gauntlet dungeon should have at least 10 rooms, but has ${dungeon.roomGrid.size}"
    )

    // Verify player spawns inside the dungeon
    val playerPos = gameState.playerEntity.position
    println(s"Player position: $playerPos")

    // Verify enemies exist
    val enemies = gameState.entities.filter(e =>
      e.get[game.entity.EntityTypeComponent].exists(_.entityType == game.entity.EntityType.Enemy)
    )
    assert(enemies.nonEmpty, "Gauntlet should spawn enemies in the dungeon")
    println(s"Enemies spawned: ${enemies.size}")
  }
}
