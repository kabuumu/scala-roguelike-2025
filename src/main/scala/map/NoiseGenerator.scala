package map

import scala.util.Random

object NoiseGenerator {
  val enumLimit = 7

  def getNoise(minX: Int, maxX: Int, minY: Int, maxY: Int, seed: Long = System.currentTimeMillis()): Map[(Int, Int), Int] = {
    val random = new Random(seed)
    val width = maxX - minX + 1
    val height = maxY - minY + 1
    
    // Use mutable map for better performance during construction
    val noiseMap = scala.collection.mutable.HashMap.empty[(Int, Int), Int]
    noiseMap.sizeHint(width * height)
    
    var currentY = minY
    while (currentY <= maxY) {
      var currentX = minX
      while (currentX <= maxX) {
        val xNeighbour = noiseMap.get((currentX - 1, currentY))
        val yNeighbour = noiseMap.get((currentX, currentY - 1))
        
        val newN = (xNeighbour, yNeighbour) match {
          case (Some(x), Some(y)) =>
            val chosenNeighbour = if (random.nextBoolean()) x else y
            noisify(chosenNeighbour, random)
          case (Some(x), None) =>
            noisify(x, random)
          case (None, Some(y)) =>
            noisify(y, random)
          case (None, None) =>
            noisify(random.nextInt(enumLimit + 1), random)
        }
        
        noiseMap((currentX, currentY)) = newN
        currentX += 1
      }
      currentY += 1
    }
    
    noiseMap.toMap
  }

  def noisify(n: Int, random: Random): Int = random.nextInt(3) match {
    case 0 => Math.max(n - 1, 0)
    case 1 => n
    case 2 => Math.min(n + 1, enumLimit)
  }
}
