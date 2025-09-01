package map

import scala.annotation.tailrec
import scala.util.Random

object NoiseGenerator {
  val enumLimit = 7

  def getNoise(minX: Int, maxX: Int, minY: Int, maxY: Int, seed: Long = System.currentTimeMillis()): Map[(Int, Int), Int] = {
    val random = new Random(seed)
    val totalSize = (maxX - minX + 1) * (maxY - minY + 1)

    @tailrec
    def loop(currentMap: Map[(Int, Int), Int] = Map.empty, currentX: Int, currentY: Int): Map[(Int, Int), Int] = {
      //print current x and current Y
      if (currentMap.size > totalSize) {
        currentMap
      } else {
        val newN = (currentMap.get((currentX - 1) -> currentY), currentMap.get(currentX -> (currentY - 1))) match {
          case (Some(xNeighbour), Some(yNeighbour)) =>
            val chosenNeighbour = if (random.nextBoolean) xNeighbour else yNeighbour
            noisify(chosenNeighbour, random) // Randomly choose between the two neighbours
          case (Some(xNeighbour), None) =>
            noisify(xNeighbour, random)
          case (None, Some(yNeighbour)) =>
            noisify(yNeighbour, random)
          case (None, None) =>
            noisify(random.nextInt(enumLimit + 1), random) // Default value for the first cell
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

  def noisify(n: Int, random: Random): Int = random.nextInt(3) match {
    case 0 => Math.max(n - 1, 0)
    case 1 => n
    case 2 => Math.min(n + 1, enumLimit)
  }
}
