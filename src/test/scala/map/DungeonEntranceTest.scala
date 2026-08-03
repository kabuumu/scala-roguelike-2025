package map

import org.scalatest.funsuite.AnyFunSuite
import game.{Direction, Point}

class DungeonEntranceTest extends AnyFunSuite {

  test("getEntranceDoor places door on wall perimeter for all 4 directions") {
    val startRoom = Point(0, 0)

    val upDoor = Dungeon.getEntranceDoor(startRoom, Direction.Up)
    val downDoor = Dungeon.getEntranceDoor(startRoom, Direction.Down)
    val leftDoor = Dungeon.getEntranceDoor(startRoom, Direction.Left)
    val rightDoor = Dungeon.getEntranceDoor(startRoom, Direction.Right)

    assert(upDoor == Point(5, 0), s"Up door should be at (5,0), got $upDoor")
    assert(downDoor == Point(5, 10), s"Down door should be at (5,10), got $downDoor")
    assert(leftDoor == Point(0, 5), s"Left door should be at (0,5), got $leftDoor")
    assert(rightDoor == Point(10, 5), s"Right door should be at (10,5), got $rightDoor")
  }

  test("dungeon tiles mark entrance door as walkable non-wall tile for Right/Down entrances") {
    val configRight = DungeonConfig(
      bounds = MapBounds(0, 3, 0, 3),
      seed = 12345L,
      entranceSide = Direction.Right
    )
    val dungeonRight = DungeonGenerator.generateDungeon(configRight)
    val rightDoor = Dungeon.getEntranceDoor(dungeonRight.startPoint, Direction.Right)

    assert(!dungeonRight.walls.contains(rightDoor), "Right entrance door must NOT be a wall")
    assert(
      dungeonRight.tiles.get(rightDoor).contains(TileType.Floor) ||
      dungeonRight.tiles.get(rightDoor).contains(TileType.Dirt) ||
      dungeonRight.tiles.get(rightDoor).contains(TileType.Bridge),
      "Entrance door tile must be walkable floor/dirt/bridge"
    )

    val configDown = DungeonConfig(
      bounds = MapBounds(0, 3, 0, 3),
      seed = 12345L,
      entranceSide = Direction.Down
    )
    val dungeonDown = DungeonGenerator.generateDungeon(configDown)
    val downDoor = Dungeon.getEntranceDoor(dungeonDown.startPoint, Direction.Down)

    assert(!dungeonDown.walls.contains(downDoor), "Down entrance door must NOT be a wall")
    assert(
      dungeonDown.tiles.get(downDoor).contains(TileType.Floor) ||
      dungeonDown.tiles.get(downDoor).contains(TileType.Dirt) ||
      dungeonDown.tiles.get(downDoor).contains(TileType.Bridge),
      "Entrance door tile must be walkable floor/dirt/bridge"
    )
  }
}
