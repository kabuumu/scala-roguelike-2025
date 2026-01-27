package scala.util

import game.entity.Movement.position
import game.{Direction, GameState, Point}

import scala.annotation.tailrec
import scala.collection.mutable

object Pathfinder {

  /** A* pathfinding algorithm for entities of arbitrary size. Note: Uses
    * mutable collections for performance. Not thread-safe. This is acceptable
    * as the game runs in a single-threaded environment.
    */
  def findPathWithSize(
      start: Point,
      end: Point,
      blockers: Set[Point],
      entitySize: Point,
      ignoredPoints: Set[Point] = Set.empty
  ): Seq[Point] = {
    def heuristic(a: Point, b: Point): Int =
      Math.abs(a.x - b.x) + Math.abs(a.y - b.y)

    case class Node(point: Point, g: Int, f: Int, parent: Option[Node])

    implicit val nodeOrdering: Ordering[Node] = Ordering.by(-_.f)

    val openSet =
      mutable.PriorityQueue(Node(start, 0, heuristic(start, end), None))

    // Track best g-score for each point to avoid expensive linear scans in openSet
    val gScore = mutable.HashMap[Point, Int](start -> 0)

    val closedSet = mutable.HashSet.empty[Point]

    // Safety limit to prevent infinite loops in unreachable scenarios
    var iterations = 0
    val MaxIterations = 6000

    // Check if all tiles for an entity of given size at position are clear
    def isPositionValid(position: Point): Boolean = {
      // Fast path for 1x1 entities
      if (entitySize.x == 1 && entitySize.y == 1) {
        !blockers.contains(position) || ignoredPoints.contains(position)
      } else {
        // For a large entity, we need to check all tiles it would occupy
        // The position represents the top-left anchor point of the entity
        val entityTiles = for {
          dx <- 0 until entitySize.x
          dy <- 0 until entitySize.y
        } yield Point(position.x + dx, position.y + dy)

        // All tiles must be clear
        entityTiles.forall { tile =>
          !blockers.contains(tile) || ignoredPoints.contains(tile)
        }
      }
    }

    def reconstructPath(node: Node): Seq[Point] = {
      @tailrec
      def loop(n: Node, acc: Seq[Point]): Seq[Point] = n.parent match {
        case Some(parent) => loop(parent, n.point +: acc)
        case None         => n.point +: acc
      }
      loop(node, Seq.empty)
    }

    while (openSet.nonEmpty) {
      iterations += 1
      if (iterations > MaxIterations) return Seq.empty // Abort if too expensive

      val current = openSet.dequeue()

      // If we found a shorter path to this node already, skip it (lazy deletion)
      if (!gScore.get(current.point).exists(_ < current.g)) {
        if (current.point == end) {
          return reconstructPath(current)
        }

        closedSet += current.point

        val neighbors = current.point.neighbors
          .filter(p => !closedSet.contains(p) && isPositionValid(p))

        neighbors.foreach { neighbor =>
          val tentativeG = current.g + 1

          // If this path is better than any previous path to neighbor
          if (tentativeG < gScore.getOrElse(neighbor, Int.MaxValue)) {
            gScore(neighbor) = tentativeG
            val f = tentativeG + heuristic(neighbor, end)
            openSet.enqueue(Node(neighbor, tentativeG, f, Some(current)))
          }
        }
      }
    }

    Seq.empty
  }

  def getNextStep(
      startPosition: Point,
      targetPosition: Point,
      gameState: GameState
  ): Option[Direction] = {
    getNextStepWithSize(
      startPosition,
      targetPosition,
      gameState,
      entitySize = Point(1, 1)
    )
  }

  def getNextStepWithSize(
      startPosition: Point,
      targetPosition: Point,
      gameState: GameState,
      entitySize: Point
  ): Option[Direction] = {
    import game.entity.Hitbox.*

    // Find the moving entity (the one at startPosition) to exclude its tiles from blockers
    val movingEntity = gameState.entities.find(_.position == startPosition)

    // Find the target entity to get its hitbox
    val targetEntity = gameState.entities.find(_.position == targetPosition)

    // Calculate all tiles that would be occupied by the moving entity at the start position
    val movingEntityTiles = movingEntity match {
      case Some(entity) => entity.hitbox
      case None         => Set(startPosition)
    }

    // Calculate all tiles occupied by the target entity
    val targetTiles = targetEntity match {
      case Some(entity) => entity.hitbox
      case None         => Set(targetPosition)
    }

    // We do NOT modify originalBlockers anymore.
    // Instead we pass the points to ignore to findPathWithSize
    val originalBlockers = gameState.movementBlockingPoints
    val ignoredPoints = movingEntityTiles ++ targetTiles

    val path = Pathfinder.findPathWithSize(
      startPosition,
      targetPosition,
      originalBlockers,
      entitySize,
      ignoredPoints
    )

    path.drop(1).headOption match {
      case Some(nextStep) =>
        val direction = Direction.fromPoints(
          startPosition,
          nextStep
        )
        Some(direction)
      case None =>
        None
    }
  }
}
