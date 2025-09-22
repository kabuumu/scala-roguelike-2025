package scala.util

import game.{Direction, GameState, Point}
import game.entity.Movement.position

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.collection.mutable

object Pathfinder {
  def findPath(start: Point, end: Point, blockers: Seq[Point]): Seq[Point] = {
    findPathWithSize(start, end, blockers, entitySize = Point(1, 1))
  }

  def findPathWithSize(start: Point, end: Point, blockers: Seq[Point], entitySize: Point): Seq[Point] = {
    def heuristic(a: Point, b: Point): Int = Math.abs(a.x - b.x) + Math.abs(a.y - b.y)

    case class Node(point: Point, g: Int, f: Int, parent: Option[Node])

    implicit val nodeOrdering: Ordering[Node] = Ordering.by(-_.f)

    val openSet = mutable.PriorityQueue(Node(start, 0, heuristic(start, end), None))
    val closedSet = HashSet.empty[Point]

    // Check if all tiles for an entity of given size at position are clear
    def isPositionValid(position: Point): Boolean = {
      val entityTiles = for {
        dx <- 0 until entitySize.x
        dy <- 0 until entitySize.y
      } yield Point(position.x + dx, position.y + dy)
      
      // All tiles must be clear (not in blockers list) for position to be valid
      !entityTiles.exists(tile => blockers.contains(tile))
    }

    def reconstructPath(node: Node): Seq[Point] = {
      @tailrec
      def loop(n: Node, acc: Seq[Point]): Seq[Point] = n.parent match {
        case Some(parent) => loop(parent, n.point +: acc)
        case None => n.point +: acc
      }
      loop(node, Seq.empty)
    }

    @tailrec
    def search(openSet: mutable.PriorityQueue[Node], closedSet: HashSet[Point]): Seq[Point] = {
      if (openSet.isEmpty) return Seq.empty

      val current = openSet.dequeue()
      if (current.point == end) return reconstructPath(current)

      val newClosedSet = closedSet + current.point
      val neighbors = current.point.neighbors
        .filter(isPositionValid) // Use the new validation that checks entity size
        .filterNot(newClosedSet.contains)

      neighbors.foreach { neighbor =>
        val tentativeG = current.g + 1
        val existingNode = openSet.find(_.point == neighbor)
        if (existingNode.isEmpty || tentativeG < existingNode.get.g) {
          openSet.enqueue(Node(neighbor, tentativeG, tentativeG + heuristic(neighbor, end), Some(current)))
        }
      }

      search(openSet, newClosedSet)
    }

    search(openSet, closedSet)
  }

  def getNextStep(startPosition: Point, targetPosition: Point, gameState: GameState): Option[Direction] = {
    getNextStepWithSize(startPosition, targetPosition, gameState, entitySize = Point(1, 1))
  }

  def getNextStepWithSize(startPosition: Point, targetPosition: Point, gameState: GameState, entitySize: Point): Option[Direction] = {
    // Find the moving entity (the one at startPosition) to exclude its tiles from blockers
    val movingEntity = gameState.entities.find(_.position == startPosition)
    
    // Find the target entity to get its hitbox
    val targetEntity = gameState.entities.find(_.position == targetPosition)
    
    // Calculate all tiles that would be occupied by the moving entity at the start position
    val movingEntityTiles = movingEntity match {
      case Some(entity) =>
        entity.get[game.entity.Hitbox] match {
          case Some(hitbox) =>
            // Multi-tile entity: include all hitbox points at current position
            hitbox.points.map(hitboxPoint => Point(startPosition.x + hitboxPoint.x, startPosition.y + hitboxPoint.y))
          case None =>
            // Single-tile entity: just the start position
            Set(startPosition)
        }
      case None =>
        // Fallback: calculate tiles based on entity size parameter
        val tiles = for {
          dx <- 0 until entitySize.x
          dy <- 0 until entitySize.y
        } yield Point(startPosition.x + dx, startPosition.y + dy)
        tiles.toSet
    }
    
    // Calculate all tiles occupied by the target entity
    val targetTiles = targetEntity match {
      case Some(entity) =>
        entity.get[game.entity.Hitbox] match {
          case Some(hitbox) =>
            // Multi-tile entity: include all hitbox points
            hitbox.points.map(hitboxPoint => Point(targetPosition.x + hitboxPoint.x, targetPosition.y + hitboxPoint.y))
          case None =>
            // Single-tile entity: just the target position
            Set(targetPosition)
        }
      case None =>
        // No entity at target position, just use the position
        Set(targetPosition)
    }
    
    // Remove both moving entity tiles and target entity tiles from blocking points
    val originalBlockers = gameState.movementBlockingPoints
    val adjustedBlockers = originalBlockers -- movingEntityTiles -- targetTiles
    
    val path = Pathfinder.findPathWithSize(
      startPosition,
      targetPosition,
      adjustedBlockers.toSeq,
      entitySize
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
  }}