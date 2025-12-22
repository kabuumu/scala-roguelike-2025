package map

import game.Point
import map.TileType

/** Shared logic for generating base terrain (height, moisture, biomes). Used by
  * both WorldMapGenerator (initial area) and ChunkManager (dynamic areas) to
  * ensure consistency.
  */
object SharedTerrainGenerator {

  /** Generates the tile type for a specific coordinate using procedural noise.
    */
  def generateTile(x: Int, y: Int, seed: Long): TileType = {
    // 1. Base Terrain (Large Scale)
    // Scale: 20.0 means noise features are ~20 tiles wide
    val heightVal = PerlinNoise.fbm(
      x.toDouble,
      y.toDouble,
      octaves = 3,
      persistence = 0.5,
      lacunarity = 2.0,
      scale = 20.0,
      seed = seed
    )

    // 2. Moisture (Large Scale)
    val moistureVal = PerlinNoise.fbm(
      x.toDouble,
      y.toDouble,
      octaves = 3,
      persistence = 0.5,
      lacunarity = 2.0,
      scale = 30.0,
      seed = seed + 54321
    )

    // 3. Detail Noise (Small Scale) - For edge roughness and local variation
    val detailNoise = PerlinNoise.fbm(
      x.toDouble,
      y.toDouble,
      octaves = 2,
      persistence = 0.5,
      lacunarity = 2.0,
      scale = 4.0, // High frequency
      seed = seed + 112233
    )

    // Combined height with detail influence
    // Adding 20% of the detail noise allows for rougher biome transitions
    val combinedHeight = heightVal + (detailNoise * 0.2)

    // 4. Feature Scattering (Very High Frequency) - For single tile variation
    val randomScatter = PerlinNoise.noise(
      x.toDouble,
      y.toDouble,
      seed + 777
    )

    // 5. River Noise
    val riverNoise = PerlinNoise.fbm(
      x.toDouble,
      y.toDouble,
      octaves = 2,
      persistence = 0.5,
      lacunarity = 2.0,
      scale = 50.0,
      seed = seed + 99999
    )
    // Modulate river width slightly with detail to make it less perfect
    val riverThreshold = 0.04 + (detailNoise * 0.01)
    val isRiver = Math.abs(riverNoise) < riverThreshold

    // Logic Tree
    if (isRiver) {
      TileType.Water
    } else if (moistureVal > 0.45 + (detailNoise * 0.05)) { // Lakes
      TileType.Water
    } else if (combinedHeight < -0.25) {
      // Dirt patches
      // Small chance for a rock
      if (randomScatter > 0.8) TileType.Rock else TileType.Dirt
    } else if (combinedHeight > 0.35) {
      // Forest
      // Small chance for a clearing (grass)
      if (randomScatter > 0.7) TileType.Grass1 else TileType.Tree
    } else {
      // Grasslands (The bulk of the terrain)

      // Use detail noise and scatter to mix grass types
      // detailNoise is approx -1.0 to 1.0

      if (detailNoise > 0.3) {
        // "Tall Grass" patches
        if (randomScatter > 0.2) TileType.Grass2 else TileType.Grass1
      } else if (detailNoise < -0.3) {
        // "Flower/Dense" patches
        if (randomScatter > 0.2) TileType.Grass3 else TileType.Grass1
      } else {
        // Standard mix
        if (randomScatter > 0.85) TileType.Grass2
        else if (randomScatter < -0.85) TileType.Grass3
        else TileType.Grass1
      }
    }
  }

  /** Batch generation for a set of points (e.g. a chunk).
    */
  def generateTiles(bounds: MapBounds, seed: Long): Map[Point, TileType] = {
    (for {
      x <- bounds.minRoomX to bounds.maxRoomX
      y <- bounds.minRoomY to bounds.maxRoomY
    } yield {
      val point = Point(x, y)
      point -> generateTile(x, y, seed)
    }).toMap
  }
}
