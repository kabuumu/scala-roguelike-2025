package map

import org.scalatest.funsuite.AnyFunSuite
import game.{Point, StartingState}
import game.entity.Movement.position

class TownDungeonRoadTest extends AnyFunSuite {

  test("Towns and dungeons have direct walkable road paths connecting them") {
    val seed = 100L
    val state = StartingState.startAdventure(seed)
    val worldMap = state.worldMap

    assert(worldMap.dungeons.nonEmpty, "Adventure mode must have dungeons")

    val dungeonApproach = Dungeon.getApproachTile(worldMap.dungeons.head.startPoint, worldMap.dungeons.head.entranceSide)
    val playerPos = state.playerEntity.position

    // Verify player spawn village to dungeon approach path consists of non-blocking walkable tiles
    val pathExists = isWalkablePath(playerPos, dungeonApproach, worldMap)
    assert(pathExists, s"Walkable road path must exist between player spawn $playerPos and dungeon approach $dungeonApproach")
  }

  test("Player spawn, Village Elder, and opening dungeon are connected by walkable paths across seeds") {
    Seq(42L, 100L, 2026L).foreach { seed =>
      val state = StartingState.startAdventure(seed)
      val worldMap = state.worldMap

      val elder = state.entities.find(_.get[game.entity.NameComponent].exists(_.name == "Elder")).flatMap(_.get[game.entity.Movement]).map(_.position).get
      val dungeonApproach = Dungeon.getApproachTile(worldMap.dungeons.head.startPoint, worldMap.dungeons.head.entranceSide)
      val playerPos = state.playerEntity.position

      assert(isWalkablePath(playerPos, elder, worldMap), s"Seed $seed: Walkable path must exist between player spawn $playerPos and Elder $elder")
      assert(isWalkablePath(elder, dungeonApproach, worldMap), s"Seed $seed: Walkable path must exist between Elder $elder and dungeon approach $dungeonApproach")
    }
  }

  private def isWalkablePath(start: Point, target: Point, worldMap: WorldMap): Boolean = {
    import scala.collection.mutable

    val minX = math.min(start.x, target.x) - 100
    val maxX = math.max(start.x, target.x) + 100
    val minY = math.min(start.y, target.y) - 100
    val maxY = math.max(start.y, target.y) + 100

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
