package map

import scala.util.Random

object PerlinNoise {

  /** Generates a noise value between -1.0 and 1.0 for the given coordinates.
    *
    * @param x
    *   X coordinate (global)
    * @param y
    *   Y coordinate (global)
    * @param seed
    *   Global seed
    * @param scale
    *   Scale of the noise (higher = zoomed in)
    * @return
    *   Noise value
    */
  def noise(x: Double, y: Double, seed: Long): Double = {
    // Simple 2D value noise interpolation for now, as full Perlin implementation is verbose.
    // Ideally we'd use a gradient noise optimization, but this should suffice for
    // tile-based generation.
    val xi = math.floor(x).toInt
    val yi = math.floor(y).toInt

    val tx = x - xi
    val ty = y - yi

    val rx0 = pseudoRandom(xi, yi, seed)
    val rx1 = pseudoRandom(xi + 1, yi, seed)
    val ry0 = pseudoRandom(xi, yi + 1, seed)
    val ry1 = pseudoRandom(xi + 1, yi + 1, seed)

    val sx = smoothstep(tx)
    val sy = smoothstep(ty)

    val nx0 = lerp(rx0, rx1, sx)
    val nx1 = lerp(ry0, ry1, sx)

    lerp(nx0, nx1, sy)
  }

  /** Generates Fractal Brownian Motion (layered noise).
    *
    * @param x
    *   X coordinate
    * @param y
    *   Y coordinate
    * @param octaves
    *   Number of layers
    * @param persistence
    *   Amplitude decay
    * @param lacunarity
    *   Frequency growth
    * @param scale
    *   Base scale
    * @param seed
    *   Global seed
    * @return
    *   Value typically between -1.0 and 1.0
    */
  def fbm(
      x: Double,
      y: Double,
      octaves: Int,
      persistence: Double,
      lacunarity: Double,
      scale: Double,
      seed: Long
  ): Double = {
    var total = 0.0
    var frequency = 1.0 / scale
    var amplitude = 1.0
    var maxValue = 0.0 // Used for normalizing result to 0.0 - 1.0

    for (i <- 0 until octaves) {
      total += noise(x * frequency, y * frequency, seed + i * 123) * amplitude
      maxValue += amplitude

      amplitude *= persistence
      frequency *= lacunarity
    }

    total // / maxValue // Normalize if needed
  }

  private def lerp(a: Double, b: Double, t: Double): Double = a + t * (b - a)

  private def smoothstep(t: Double): Double = t * t * (3 - 2 * t)

  private def pseudoRandom(x: Int, y: Int, seed: Long): Double = {
    // Hash based pseudo random based on x,y and seed
    // Returns value between -1.0 and 1.0
    var h = seed ^ x * 374761393 ^ y * 668265263
    h = (h ^ (h >> 13)) * 1274126177
    val v = (h ^ (h >> 16))

    // Normalize to -1.0 to 1.0
    (v & 0xffffff).toDouble / 0xffffff * 2.0 - 1.0
  }
}
