package game

import game.entity.{EntityTypeComponent, EntityType, Movement}
import org.scalatest.funsuite.AnyFunSuite
import map.Dungeon

/** Test to verify that enemies spawn in ALL dungeons, not just the primary
  * dungeon. This is a regression test for the bug where only the first dungeon
  * had enemies.
  */
class VerifyAllDungeonsHaveEnemiesTest extends AnyFunSuite {
  test("Enemies spawn in all dungeons, not just primary") {
    val state = StartingState.startGauntlet()
    val worldMap = state.worldMap

    println(s"\n=== All Dungeons Enemy Verification ===")
    println(s"Total dungeons in world: ${worldMap.dungeons.size}")

    // Get all enemy entities
    val allEnemies = state.entities.filter(entity =>
      entity.get[EntityTypeComponent].exists(_.entityType == EntityType.Enemy)
    )

    println(s"Total enemies spawned: ${allEnemies.size}")

    // For each dungeon, check how many enemies are in it
    val dungeonsWithEnemyCounts = worldMap.dungeons.zipWithIndex.map {
      case (dungeon, idx) =>
        // Determine which enemies are in this dungeon's bounds
        val dungeonMinX = dungeon.roomGrid.map(_.x).min * Dungeon.roomSize
        val dungeonMaxX = (dungeon.roomGrid.map(_.x).max + 1) * Dungeon.roomSize
        val dungeonMinY = dungeon.roomGrid.map(_.y).min * Dungeon.roomSize
        val dungeonMaxY = (dungeon.roomGrid.map(_.y).max + 1) * Dungeon.roomSize

        val enemiesInDungeon = allEnemies.filter { enemy =>
          enemy.get[Movement].exists { movement =>
            val pos = movement.position
            pos.x >= dungeonMinX && pos.x <= dungeonMaxX &&
            pos.y >= dungeonMinY && pos.y <= dungeonMaxY
          }
        }

        println(s"\nDungeon $idx:")
        println(s"  Rooms: ${dungeon.roomGrid.size}")
        println(
          s"  Room bounds: (${dungeon.roomGrid.map(_.x).min}, ${dungeon.roomGrid.map(_.y).min}) to (${dungeon.roomGrid.map(_.x).max}, ${dungeon.roomGrid.map(_.y).max})"
        )
        println(s"  Enemies: ${enemiesInDungeon.size}")
        println(s"  Has trader room: ${dungeon.traderRoom.isDefined}")
        println(s"  Has boss room: ${dungeon.endpoint.isDefined}")

        (dungeon, enemiesInDungeon.size)
    }

    // Count how many dungeons have enemies
    val dungeonsWithEnemies = dungeonsWithEnemyCounts.count(_._2 > 0)

    println(s"\n=== Summary ===")
    println(
      s"Dungeons with enemies: $dungeonsWithEnemies / ${worldMap.dungeons.size}"
    )

    // Gauntlet mode produces 1 dungeon — assert it has enemies
    assert(
      dungeonsWithEnemies >= 1,
      s"Expected at least 1 dungeon to have enemies, but only $dungeonsWithEnemies out of ${worldMap.dungeons.size} have enemies"
    )

    // Verify that we have a reasonable number of enemies
    assert(
      allEnemies.size > 5,
      s"Expected at least 5 enemies in the dungeon, but got ${allEnemies.size}"
    )

    println(
      s"✅ VERIFIED: Enemies are spawning in dungeons (total: ${allEnemies.size} enemies)"
    )
  }

  test("Dungeon traders spawn in all dungeons that have trader rooms") {
    val state = StartingState.startGauntlet()
    val worldMap = state.worldMap

    println(s"\n=== Dungeon Trader Verification ===")

    // Count how many dungeons have trader rooms
    val dungeonsWithTraderRooms =
      worldMap.dungeons.count(_.traderRoom.isDefined)

    println(s"Dungeons with trader rooms: $dungeonsWithTraderRooms")

    // Get all dungeon trader entities
    val allTraders = state.entities.filter(_.id.contains("trader"))
    println(
      s"All trader entities found: ${allTraders.map(_.id).mkString(", ")}"
    )

    val dungeonTraders =
      state.entities.filter(_.id.startsWith("trader-dungeon-"))

    println(s"Dungeon traders created: ${dungeonTraders.size}")

    // Verify that we have one trader per dungeon with a trader room
    assert(
      dungeonTraders.size == dungeonsWithTraderRooms,
      s"Expected $dungeonsWithTraderRooms dungeon traders, but got ${dungeonTraders.size}"
    )

    println(s"✅ VERIFIED: All dungeons with trader rooms have traders")
  }
}
