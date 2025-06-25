package map

import scala.annotation.tailrec
import scala.util.Random

object NoiseGenerator {
  val enumLimit = 7

  (for {
    _ <- 1 to 1000
    n = noisify(1)
  } yield n).groupBy(identity).foreach {
    case (n, occurrences) =>
      println(s"Value: $n, Occurrences: ${occurrences.size}")
  }

  def getNoise(minX: Int, maxX: Int, minY: Int, maxY: Int): Map[(Int, Int), Int] = {
    val totalSize = (maxX - minX + 1) * (maxY - minY + 1)

    @tailrec
    def loop(currentMap: Map[(Int, Int), Int] = Map.empty, currentX: Int, currentY: Int): Map[(Int, Int), Int] = {
      //print current x and current Y
      if (currentMap.size > totalSize) {
        currentMap
      } else {
        val newN = (currentMap.get((currentX - 1) -> currentY), currentMap.get(currentX -> (currentY - 1))) match {
          case (Some(xNeighbour), Some(yNeighbour)) =>
            val chosenNeighbour = if (Random.nextBoolean) xNeighbour else yNeighbour
            noisify(chosenNeighbour) // Randomly choose between the two neighbours
          case (Some(xNeighbour), None) =>
            noisify(xNeighbour)
          case (None, Some(yNeighbour)) =>
            noisify(yNeighbour)
          case (None, None) =>
            noisify(Random.nextInt(enumLimit + 1)) // Default value for the first cell
        }
        val newCoordinates = (currentX, currentY)
        val newMap = currentMap + (newCoordinates -> newN)
        val newX = if (currentX == maxX) minX else currentX + 1
        val newY = if (currentX == maxX) currentY + 1 else currentY

        loop(newMap, newX, newY)
      }
    }

    loop(Map.empty, minX, minY)
  }

  def noisify(n: Int): Int = Random.nextInt(3) match {
    case 0 => Math.max(n - 1, 0)
    case 1 => n
    case 2 => Math.min(n + 1, enumLimit)
  }
}
