package map

import org.scalatest.funsuite.AnyFunSuite
import game.{Point, StartingState}
import game.entity.Movement.position

class DungeonConnectivityTest extends AnyFunSuite {

  test("every dungeon in Adventure mode has a clear walkable path from spawn village across seeds") {
    val seed = 100L
    val gameState = StartingState.startAdventure(seed)
    val playerPos = gameState.playerEntity.position
    val worldMap = gameState.worldMap

    assert(worldMap.dungeons.nonEmpty, s"World map for seed $seed must contain dungeons")

    worldMap.dungeons.foreach { dungeon =>
      val entranceDoor = Dungeon.getEntranceDoor(dungeon.startPoint, dungeon.entranceSide)
      val approachTile = Dungeon.getApproachTile(dungeon.startPoint, dungeon.entranceSide)

      assert(!worldMap.staticMovementBlockingPoints.contains(entranceDoor),
        s"Seed $seed: Entrance door $entranceDoor must be walkable")
      assert(!worldMap.staticMovementBlockingPoints.contains(approachTile),
        s"Seed $seed: Approach tile $approachTile must be walkable")

      val reachable = isReachable(playerPos, approachTile, worldMap)
      assert(reachable, s"Seed $seed: Dungeon entrance $approachTile must be reachable from player spawn $playerPos")
    }
  }

  private def isReachable(start: Point, target: Point, worldMap: WorldMap): Boolean = {
    import scala.collection.mutable

    val minX = math.min(start.x, target.x) - 60
    val maxX = math.max(start.x, target.x) + 60
    val minY = math.min(start.y, target.y) - 60
    val maxY = math.max(start.y, target.y) + 60

    def isWalkable(p: Point): Boolean = {
      worldMap.getTile(p) match {
        case Some(TileType.Wall) | Some(TileType.Rock) | Some(TileType.Tree) => false
        case Some(TileType.Water) => worldMap.bridges.contains(p)
        case _ => true
      }
    }

    val queue = mutable.Queue[Point](start)
    val visited = mutable.Set[Point](start)

    while (queue.nonEmpty && visited.size < 50000) {
      val curr = queue.dequeue()
      if (curr == target) return true

      val neighbors = Seq(
        Point(curr.x + 1, curr.y),
        Point(curr.x - 1, curr.y),
        Point(curr.x, curr.y + 1),
        Point(curr.x, curr.y - 1)
      )

      neighbors.foreach { n =>
        if (n.x >= minX && n.x <= maxX && n.y >= minY && n.y <= maxY) {
          if (!visited.contains(n) && isWalkable(n)) {
            visited += n
            queue.enqueue(n)
          }
        }
      }
    }

    false
  }
}
