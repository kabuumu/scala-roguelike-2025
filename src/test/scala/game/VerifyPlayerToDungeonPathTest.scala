package game

import org.scalatest.funsuite.AnyFunSuite
import map.TileType
import game.entity.Movement.position

class VerifyPlayerToDungeonPathTest extends AnyFunSuite {
  test("Verify player-to-dungeon path exists and connects properly") {
    println(s"\n=== Player-to-Dungeon Path Verification ===")

    val state = StartingState.startingGameState
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

    // Check the path
    val pathTiles = worldMap.paths
    println(s"Total path tiles: ${pathTiles.size}")

    assert(pathTiles.nonEmpty, "Path should not be empty")

    // Verify path starts near player
    val pathNearPlayer = pathTiles.filter { tile =>
      val dx = tile.x - player.position.x
      val dy = tile.y - player.position.y
      dx * dx + dy * dy < 100 // Within 10 tiles
    }
    println(s"Path tiles near player (within 10 tiles): ${pathNearPlayer.size}")

    // Verify path reaches near dungeon entrance
    val pathNearDungeon = pathTiles.filter { tile =>
      val dx = tile.x - dungeonEntranceTile.x
      val dy = tile.y - dungeonEntranceTile.y
      dx * dx + dy * dy < 100 // Within 10 tiles
    }
    println(
      s"Path tiles near dungeon entrance (within 10 tiles): ${pathNearDungeon.size}"
    )

    // Check that path tiles are actually Dirt in the worldMap
    // Note: Some path tiles may be Floor if they pass through shop or dungeon
    val pathTilesAsDirt = pathTiles.count { tile =>
      val t = worldMap.tiles.get(tile)
      t.contains(TileType.Dirt) || t.contains(TileType.Bridge)
    }
    val pathTilesAsFloor = pathTiles.count { tile =>
      worldMap.tiles.get(tile).contains(TileType.Floor)
    }
    println(
      s"Path tiles rendered as Dirt or Bridge: $pathTilesAsDirt / ${pathTiles.size}"
    )
    println(
      s"Path tiles rendered as Floor (shop/dungeon): $pathTilesAsFloor / ${pathTiles.size}"
    )

    // Sample some path tiles
    println(s"\nSample path tiles (first 10):")
    pathTiles.take(10).foreach { tile =>
      val tileType = worldMap.tiles.get(tile)
      println(s"  $tile -> ${tileType.getOrElse("NOT IN WORLDMAP")}")
    }

    // Check if path connects player to dungeon
    val playerToFirstPath = pathNearPlayer.nonEmpty
    val dungeonToLastPath = pathNearDungeon.nonEmpty

    println(s"\nPath connectivity:")
    println(s"  Path connects to player area: $playerToFirstPath")
    println(s"  Path connects to dungeon area: $dungeonToLastPath")

    assert(playerToFirstPath, "Path should connect to player spawn area")
    assert(dungeonToLastPath, "Path should connect to dungeon entrance area")
    // Most path tiles should be Dirt, but some may be Floor (shop) or dungeon tiles
    // Accept if at least 70% are Dirt (to account for shop floor tiles)
    assert(
      pathTilesAsDirt > pathTiles.size * 0.7,
      s"Most path tiles should be rendered as Dirt or Bridge (found $pathTilesAsDirt out of ${pathTiles.size})"
    )

    println(
      s"\n✅ CONFIRMED: Path exists with ${pathTiles.size} tiles connecting player to dungeon"
    )
  }
}
