package game

import scala.annotation.tailrec
import scala.collection.immutable.HashSet
import scala.collection.mutable

object Pathfinder {
  def findPath(start: Point, end: Point, blockers: Seq[Point]): Seq[Point] = {
    def heuristic(a: Point, b: Point): Int = Math.abs(a.x - b.x) + Math.abs(a.y - b.y)

    case class Node(point: Point, g: Int, f: Int, parent: Option[Node])

    implicit val nodeOrdering: Ordering[Node] = Ordering.by(-_.f)

    val openSet = mutable.PriorityQueue(Node(start, 0, heuristic(start, end), None))
    val closedSet = HashSet.empty[Point]

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
      val neighbors = current.point.neighbors.filterNot(blockers.contains).filterNot(newClosedSet.contains)

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
}