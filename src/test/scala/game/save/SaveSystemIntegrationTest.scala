package game.save

import org.scalatest.funsuite.AnyFunSuite
import game.GameState
import map.{Dungeon, RoomConnection, WorldMap, MapBounds}
import game.Point
import game.Direction
import game.GameMode
import game.entity.Entity

class SaveSystemIntegrationTest extends AnyFunSuite {

  test("Save system must preserve dungeon structure including connections") {
    // 1. Setup a dungeon with explicit connections
    val startRoom = Point(0, 0)
    val nextRoom = Point(1, 0)

    val connection = RoomConnection(startRoom, Direction.Right, nextRoom)
    val backConnection = RoomConnection(nextRoom, Direction.Left, startRoom)

    val originalDungeon = Dungeon(
      roomGrid = Set(startRoom, nextRoom),
      roomConnections = Set(connection, backConnection),
      startPoint = startRoom,
      endpoint = Some(nextRoom),
      seed = 12345L
    )

    // Verify original dungeon has connected paths in its tiles
    // If usage of roomConnections is correct in Dungeon.scala, tiles should reflect paths
    val startTile = Dungeon.roomToTile(startRoom)
    val nextTile = Dungeon.roomToTile(nextRoom)

    // Check for a floor tile between the centers (the path)
    // Center of start room (0,0) is (5,5). Center of next room (1,0) is (15,5).
    // Path should exist at (10,5).
    val pathPoint = Point(10, 5)

    // Ensure the original dungeon actually put a floor there (proving connections are used)
    assert(
      originalDungeon.tiles.contains(pathPoint),
      "Original dungeon should have a path between rooms"
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
      entities =
        Vector(Entity(id = "player", components = Map.empty)), // Minimal player
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

    // Fails here if connections are lost
    assert(
      restoredDungeon.roomConnections.nonEmpty,
      "Restored dungeon should have room connections"
    )
    assert(
      restoredDungeon.endpoint.isDefined,
      "Restored dungeon should have an endpoint"
    )
    assert(
      restoredDungeon.tiles.contains(pathPoint),
      "Restored dungeon should have path tiles generated from connections"
    )
  }
}
