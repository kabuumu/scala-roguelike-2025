package map

import game.Point
import game.Direction
import org.scalatest.funsuite.AnyFunSuite

class DungeonEntranceTest extends AnyFunSuite {

  test("Dungeon Entrance Logic - Approach Tile should be outside bounds") {
    // Define a 3x3 dungeon at (10, 10) rooms -> (100, 100) tiles
    // RoomSize = 10
    val startRoom = Point(10, 10)

    // Bounds: 100 to 129 (3 rooms * 10 - 1? No, 3 rooms: 10, 11, 12.)
    // Min X: 100, Max X: 129 (inclusive)
    val minTileX = 100
    val maxTileX = 129
    val minTileY = 100
    val maxTileY = 129

    // Test Left Entrance
    val entranceLeft = Direction.Left
    val doorLeft = Dungeon.getEntranceDoor(startRoom, entranceLeft)
    val approachLeft = Dungeon.getApproachTile(startRoom, entranceLeft)

    println(s"Left - Door: $doorLeft, Approach: $approachLeft")

    // Expected: Door on wall (100?), Approach outside (99?)
    // Door logic usually centers on the wall.
    // Room 10's left wall is at x=100.

    assert(
      doorLeft.x >= minTileX,
      s"Door Left $doorLeft should be inside/on boundary $minTileX"
    )
    assert(
      approachLeft.x < minTileX,
      s"Approach Left $approachLeft should be strictly outside boundary $minTileX"
    )

    // Test Right Entrance
    // If dungeon is 1x1 only for start room?
    // Let's assume startRoom IS the edge room.
    val entranceRight = Direction.Right
    val doorRight = Dungeon.getEntranceDoor(startRoom, entranceRight)
    val approachRight = Dungeon.getApproachTile(startRoom, entranceRight)

    println(s"Right - Door: $doorRight, Approach: $approachRight")

    // Right wall of room 10 is at x=109.
    // Door should be 109. Approach 110.
    assert(
      doorRight.x <= minTileX + 9,
      "Door Right should be within room"
    ) // Actually door is usually ON the wall
    assert(
      approachRight.x > minTileX + 9,
      "Approach Right should be outside room"
    )
  }
}
