package game.save

import org.scalatest.funsuite.AnyFunSuite
import game.GameState
import map.{Dungeon, WorldMap, MapBounds}
import game.Point
import game.GameMode
import game.entity.Entity

class SaveSystemTraderRoomTest extends AnyFunSuite {

  test(
    "Save system must persist traderRoom and hasBossRoom to ensure consistent tile generation"
  ) {
    // 1. Setup a dungeon with a trader room
    val startRoom = Point(0, 0)
    val traderRoom = Point(1, 0)

    val originalDungeon = Dungeon(
      roomGrid = Set(startRoom, traderRoom),
      startPoint = startRoom,
      traderRoom = Some(traderRoom),
      hasBossRoom = true,
      seed = 12345L
    )

    // Wrap in GameState
    val worldMap = WorldMap(
      tiles = originalDungeon.tiles,
      dungeons = Seq(originalDungeon),
      paths = Set.empty,
      bridges = Set.empty,
      bounds = MapBounds(0, 20, 0, 10)
    )

    val originalState = GameState(
      playerEntityId = "player",
      entities = Vector(Entity(id = "player", components = Map.empty)),
      worldMap = worldMap,
      dungeonFloor = 1,
      gameMode = GameMode.Adventure
    )

    // 2. Serialize
    val json = SaveGameJson.serialize(originalState)

    // 3. Deserialize
    val restoredState = SaveGameJson.deserialize(json)

    // 4. Verify
    val restoredDungeon = restoredState.worldMap.primaryDungeon.get

    // These assertions are expected to fail until the fix is applied
    assert(
      restoredDungeon.traderRoom == originalDungeon.traderRoom,
      s"Trader room lost! Expected ${originalDungeon.traderRoom}, got ${restoredDungeon.traderRoom}"
    )

    assert(
      restoredDungeon.hasBossRoom == originalDungeon.hasBossRoom,
      s"hasBossRoom lost! Expected ${originalDungeon.hasBossRoom}, got ${restoredDungeon.hasBossRoom}"
    )
  }
}
