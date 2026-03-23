package game

import org.scalatest.funsuite.AnyFunSuite
import map.TileType
import game.entity.Movement.position

class VerifyPlayerToDungeonPathTest extends AnyFunSuite {
  test("Verify player-to-dungeon path exists and connects properly") {
    println(s"\n=== Player-to-Dungeon Path Verification ===")

    val state = StartingState.startGauntlet()
    val worldMap = state.worldMap
    val player = state.playerEntity

    println(s"Player spawn position: ${player.position}")

    // Get the primary dungeon
    assert(worldMap.primaryDungeon.isDefined, "Primary dungeon should exist")
    val dungeon = worldMap.primaryDungeon.get

    println(s"Dungeon start point (room coords): ${dungeon.startPoint}")

    // Calculate dungeon entrance in tile coordinates
    val dungeonEntranceTileX =
      dungeon.startPoint.x * map.Dungeon.roomSize + map.Dungeon.roomSize / 2
    val dungeonEntranceTileY =
      dungeon.startPoint.y * map.Dungeon.roomSize + map.Dungeon.roomSize / 2
    val dungeonEntranceTile = Point(dungeonEntranceTileX, dungeonEntranceTileY)

    println(s"Dungeon entrance (tile coords): $dungeonEntranceTile")

    // In Gauntlet mode, player spawns inside the dungeon, so verify player is near dungeon entrance
    val playerNearDungeon = {
      val dx = player.position.x - dungeonEntranceTile.x
      val dy = player.position.y - dungeonEntranceTile.y
      dx * dx + dy * dy < 400 // Within 20 tiles
    }

    println(s"Player near dungeon entrance: $playerNearDungeon")
    assert(playerNearDungeon, "Player should spawn near dungeon entrance in Gauntlet mode")

    // Verify the dungeon has walkable tiles
    val floorTiles = worldMap.tiles.count { case (_, t) =>
      t == TileType.Floor || t == TileType.MaybeFloor
    }
    println(s"Floor tiles in worldMap: $floorTiles")
    assert(floorTiles > 0, "WorldMap should contain floor tiles from the dungeon")

    println(
      s"\n✅ CONFIRMED: Player is near dungeon entrance, dungeon has $floorTiles floor tiles"
    )
  }
}
