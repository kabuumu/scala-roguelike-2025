package map

import game.Point

import scala.collection.mutable

object RoomGridPathfinder {
  def findPath(rooms: Set[Point], roomConnections: Set[RoomConnection], startPoint: Point, target: Point): Seq[RoomConnection] = {
    // Map for quick lookup of connections by origin room
    val connectionsByOrigin: Map[Point, Set[RoomConnection]] =
      roomConnections.groupBy(_.originRoom).view.mapValues(_.toSet).toMap

    // Heuristic function: Manhattan distance
    def heuristic(point: Point, target: Point): Int =
      Math.abs(point.x - target.x) + Math.abs(point.y - target.y)

    // Priority queue for open set (sorted by fScore)
    val openSet = mutable.PriorityQueue[(Point, Int)]()(Ordering.by(-_._2))
    openSet.enqueue((startPoint, 0))
        
    // Maps to store gScore (cost from start) and fScore (estimated total cost)
    val gScore = mutable.Map(startPoint -> 0).withDefaultValue(Int.MaxValue)
    val fScore = mutable.Map(startPoint -> heuristic(startPoint, target)).withDefaultValue(Int.MaxValue)

    // Map to reconstruct the path
    val cameFrom = mutable.Map[Point, RoomConnection]()

    while (openSet.nonEmpty) {
      val (current, _) = openSet.dequeue()

      // If we reached the target, reconstruct the path
      if (current == target) {
        val path = mutable.ListBuffer[RoomConnection]()
        var node = target
        while (cameFrom.contains(node)) {
          val connection = cameFrom(node)
          path.prepend(connection)
          node = connection.originRoom
        }
        return path.toSeq
      }

      // Explore neighbors
      for (connection <- connectionsByOrigin.getOrElse(current, Set.empty)) {
        val neighbor = connection.destinationRoom
        val tentativeGScore = gScore(current) + 1 // Assume uniform cost for each connection

        if (tentativeGScore < gScore(neighbor)) {
          // Update scores and enqueue neighbor
          cameFrom(neighbor) = connection
          gScore(neighbor) = tentativeGScore
          fScore(neighbor) = tentativeGScore + heuristic(neighbor, target)
          if (!openSet.exists(_._1 == neighbor)) {
            openSet.enqueue((neighbor, fScore(neighbor)))
          }
        }
      }
    }

    // If no path is found, return an empty sequence
    Seq.empty
  }
}