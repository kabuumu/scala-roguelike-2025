package scala.util

import game.{Direction, GameState, Point}

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
      
      entityTiles.forall(tile => !blockers.contains(tile))
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
    val path = Pathfinder.findPathWithSize(
      startPosition,
      targetPosition,
      (gameState.movementBlockingPoints - targetPosition).toSeq,
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