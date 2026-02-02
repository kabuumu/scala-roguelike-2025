package map

import game.Point
import scala.util.Random

/** Configuration for overworld map generation.
  *
  * @param width
  *   Width of the overworld map in pixels
  * @param height
  *   Height of the overworld map in pixels
  * @param seed
  *   Random seed for deterministic generation
  * @param landThreshold
  *   Noise threshold for land/water (0.0-1.0, higher = less land)
  * @param numCities
  *   Number of cities to place
  * @param numTowns
  *   Number of towns to place
  * @param numVillages
  *   Number of villages to place
  */
case class OverworldMapConfig(
    width: Int = 200,
    height: Int = 200,
    seed: Long = System.currentTimeMillis(),
    landThreshold: Double = 0.1, // Lower = more land (was 0.35)
    numCities: Int = 3,
    numTowns: Int = 8,
    numVillages: Int = 20
)

/** Result of overworld map generation.
  *
  * @param tiles
  *   Map from Point to OverworldTileType
  * @param seed
  *   Seed used for generation
  * @param width
  *   Width of the map
  * @param height
  *   Height of the map
  */
case class OverworldMap(
    tiles: Map[Point, OverworldTileType],
    seed: Long,
    width: Int,
    height: Int
) {
  def getTile(point: Point): Option[OverworldTileType] = tiles.get(point)
}

/** Generator for zoomed-out overworld maps with realistic landmass, biomes, and
  * settlements.
  */
object OverworldMapGenerator {

  /** Scale factor compared to the regular WorldMap (10x zoomed out) */
  val ScaleFactor: Int = 10

  /** Generates a complete overworld map.
    *
    * @param config
    *   Configuration for map generation
    * @return
    *   Generated OverworldMap
    */
  def generate(config: OverworldMapConfig): OverworldMap = {
    val random = new Random(config.seed)

    // Step 1: Generate base terrain using Perlin noise
    val baseTerrain = generateBaseTerrain(config)

    // Step 2: Apply biomes based on temperature/moisture
    val withBiomes = applyBiomes(baseTerrain, config)

    // Step 3: Place settlements and get city/town/village locations
    val (withSettlements, cityLocations, townLocations, villageLocations) =
      placeSettlementsWithLocations(withBiomes, config, random)

    // Step 4: Generate roads between cities
    val withRoads = generateRoads(withSettlements, cityLocations, config)

    // Step 5: Generate paths from towns to nearest roads
    val withPaths = generatePaths(withRoads, townLocations, config)

    // Step 6: Generate trails from villages to nearest connection
    val withTrails = generateTrails(withPaths, villageLocations, config)

    OverworldMap(
      tiles = withTrails,
      seed = config.seed,
      width = config.width,
      height = config.height
    )
  }

  /** Generates base terrain (land vs water) using Perlin noise. */
  private def generateBaseTerrain(
      config: OverworldMapConfig
  ): Map[Point, OverworldTileType] = {
    val tiles = scala.collection.mutable.Map[Point, OverworldTileType]()

    // Use multiple noise layers for more interesting landmass
    val continentScale = 80.0 // Large-scale continent shapes
    val detailScale = 30.0 // Smaller detail for coastlines
    val islandScale = 15.0 // Island-sized features

    for {
      x <- 0 until config.width
      y <- 0 until config.height
    } {
      // Primary continent noise (large shapes)
      val continentNoise = PerlinNoise.fbm(
        x.toDouble,
        y.toDouble,
        octaves = 4,
        persistence = 0.5,
        lacunarity = 2.0,
        scale = continentScale,
        seed = config.seed
      )

      // Detail noise for coastlines
      val detailNoise = PerlinNoise.fbm(
        x.toDouble,
        y.toDouble,
        octaves = 3,
        persistence = 0.4,
        lacunarity = 2.2,
        scale = detailScale,
        seed = config.seed + 1000
      )

      // Island noise for scattered islands
      val islandNoise = PerlinNoise.fbm(
        x.toDouble,
        y.toDouble,
        octaves = 2,
        persistence = 0.6,
        lacunarity = 2.0,
        scale = islandScale,
        seed = config.seed + 2000
      )

      // River noise (ridged multifractal-ish: generally thin lines)
      // We use absolute value to create ridges, then invert
      val riverRaw = PerlinNoise.fbm(
        x.toDouble,
        y.toDouble,
        octaves = 4,
        persistence = 0.5,
        lacunarity = 2.0,
        scale = 35.0, // Scale for river networks
        seed = config.seed + 3000
      )
      val riverNoise = Math.abs(riverRaw) // Create ridges close to 0

      // Combine noise layers with weights
      val combinedNoise =
        continentNoise * 0.6 + detailNoise * 0.25 + islandNoise * 0.15

      // Continental mask: force water at edges, land in center
      val centerX = config.width / 2.0
      val centerY = config.height / 2.0
      val normalizedX = (x - centerX) / centerX // -1 to 1
      val normalizedY = (y - centerY) / centerY // -1 to 1

      // Use maximum of X and Y distance for rectangular falloff
      val edgeDistX = Math.abs(normalizedX)
      val edgeDistY = Math.abs(normalizedY)
      val edgeDist = Math.max(edgeDistX, edgeDistY)

      // Strong continental falloff:
      // - 0.0 to 0.6: fully land-favored (center 60%)
      // - 0.6 to 0.85: gradual transition
      // - 0.85 to 1.0: forced ocean (outer 15% of each edge)
      val continentMask = if (edgeDist > 0.85) {
        // Force ocean at edges
        -1.0
      } else if (edgeDist > 0.6) {
        // Gradual falloff from land to water
        val falloffProgress = (edgeDist - 0.6) / 0.25
        1.0 - (falloffProgress * 1.5) // Drops from 1.0 to -0.5
      } else {
        // Center: boost land probability significantly
        1.0 + (1.0 - edgeDist / 0.6) * 0.3 // Boost center even more
      }

      val finalNoise = combinedNoise * 0.5 + continentMask * 0.5

      // Determine tile type based on noise value
      var tileType =
        if (finalNoise < config.landThreshold - 0.3) {
          OverworldTileType.Ocean
        } else if (finalNoise < config.landThreshold - 0.1) {
          OverworldTileType.Water
        } else if (finalNoise < config.landThreshold + 0.05) {
          OverworldTileType.Beach
        } else {
          OverworldTileType.Plains // Will be refined with biomes
        }

      // Apply rivers: Only on land (Plains/Beach) and not too close to edges
      // User request: "ensure more rivers or water features in the central section"
      if (
        (tileType == OverworldTileType.Plains || tileType == OverworldTileType.Beach) && edgeDist < 0.85
      ) {
        // Wider threshold in center (more water features), narrower further out
        val riverThreshold = if (edgeDist < 0.5) 0.08 else 0.04

        if (riverNoise < riverThreshold) {
          tileType = OverworldTileType.Water
        }
      }

      tiles(Point(x, y)) = tileType
    }

    tiles.toMap
  }

  /** Applies biome distribution based on temperature and moisture gradients. */
  private def applyBiomes(
      terrain: Map[Point, OverworldTileType],
      config: OverworldMapConfig
  ): Map[Point, OverworldTileType] = {
    val temperatureScale = 100.0
    val moistureScale = 60.0
    val elevationScale = 40.0

    terrain.map { case (point, tileType) =>
      // Only apply biomes to land tiles (Plains)
      if (tileType != OverworldTileType.Plains) {
        point -> tileType
      } else {
        // Temperature: gradient from top (cold) to bottom (hot) + noise
        val baseTemp = point.y.toDouble / config.height
        val tempNoise = PerlinNoise.fbm(
          point.x.toDouble,
          point.y.toDouble,
          octaves = 3,
          persistence = 0.5,
          lacunarity = 2.0,
          scale = temperatureScale,
          seed = config.seed + 3000
        )
        val temperature = (baseTemp + tempNoise * 0.3).max(0.0).min(1.0)

        // Moisture: noise-based
        val moisture = (PerlinNoise.fbm(
          point.x.toDouble,
          point.y.toDouble,
          octaves = 4,
          persistence = 0.5,
          lacunarity = 2.0,
          scale = moistureScale,
          seed = config.seed + 4000
        ) + 1.0) / 2.0 // Normalize to 0-1

        // Elevation: noise-based for mountains
        val elevation = PerlinNoise.fbm(
          point.x.toDouble,
          point.y.toDouble,
          octaves = 3,
          persistence = 0.6,
          lacunarity = 2.0,
          scale = elevationScale,
          seed = config.seed + 5000
        )

        // Determine biome based on temperature, moisture, and elevation
        val biome =
          if (elevation > 0.5) {
            OverworldTileType.Mountain
          } else if (temperature > 0.7 && moisture < 0.3) {
            OverworldTileType.Desert
          } else if (moisture > 0.5 && temperature < 0.7) {
            OverworldTileType.Forest
          } else {
            OverworldTileType.Plains
          }

        point -> biome
      }
    }
  }

  /** Places settlements on valid land tiles with proper spacing. Returns the
    * updated terrain, city locations, town locations, and village locations.
    */
  private def placeSettlementsWithLocations(
      terrain: Map[Point, OverworldTileType],
      config: OverworldMapConfig,
      random: Random
  ): (Map[Point, OverworldTileType], Seq[Point], Seq[Point], Seq[Point]) = {
    val mutableTerrain =
      scala.collection.mutable.Map[Point, OverworldTileType]()
    mutableTerrain ++= terrain

    // Find valid settlement locations (land tiles that aren't mountains)
    val validLocations = terrain
      .filter { case (_, tileType) =>
        tileType == OverworldTileType.Plains ||
        tileType == OverworldTileType.Forest
      }
      .keys
      .toSeq

    if (validLocations.isEmpty) {
      return (terrain, Seq.empty, Seq.empty, Seq.empty)
    }

    // Settlement sizes (radius from center)
    val cityRadius = 2 // 5x5 tiles
    val townRadius = 1 // 3x3 tiles
    val villageRadius = 0 // 1x1 tile (but we'll make it 2x2)

    // Minimum spacing between settlements (increased to account for size)
    val citySpacing = 50
    val townSpacing = 30
    val villageSpacing = 18

    var placedSettlements = Seq.empty[Point]

    // Helper to place a cluster of tiles for a settlement
    def placeCluster(
        center: Point,
        radius: Int,
        tileType: OverworldTileType
    ): Unit = {
      for {
        dx <- -radius to radius
        dy <- -radius to radius
      } {
        val p = Point(center.x + dx, center.y + dy)
        // Only place if within map bounds and on valid land
        if (p.x >= 0 && p.x < config.width && p.y >= 0 && p.y < config.height) {
          val existingTile = terrain.get(p)
          if (
            existingTile.exists(t =>
              t == OverworldTileType.Plains || t == OverworldTileType.Forest
            )
          ) {
            mutableTerrain(p) = tileType
          }
        }
      }
    }

    // Place cities first (most restrictive, largest)
    val cities = placeSettlementType(
      validLocations,
      config.numCities,
      citySpacing,
      placedSettlements,
      random
    )
    cities.foreach(p => placeCluster(p, cityRadius, OverworldTileType.City))
    placedSettlements = placedSettlements ++ cities

    // Place towns
    val towns = placeSettlementType(
      validLocations,
      config.numTowns,
      townSpacing,
      placedSettlements,
      random
    )
    towns.foreach(p => placeCluster(p, townRadius, OverworldTileType.Town))
    placedSettlements = placedSettlements ++ towns

    // Place villages (2x2 tiles)
    val villages = placeSettlementType(
      validLocations,
      config.numVillages,
      villageSpacing,
      placedSettlements,
      random
    )
    villages.foreach { p =>
      // 2x2 cluster for villages
      for {
        dx <- 0 to 1
        dy <- 0 to 1
      } {
        val vp = Point(p.x + dx, p.y + dy)
        if (
          vp.x >= 0 && vp.x < config.width && vp.y >= 0 && vp.y < config.height
        ) {
          val existingTile = terrain.get(vp)
          if (
            existingTile.exists(t =>
              t == OverworldTileType.Plains || t == OverworldTileType.Forest
            )
          ) {
            mutableTerrain(vp) = OverworldTileType.Village
          }
        }
      }
    }

    (mutableTerrain.toMap, cities, towns, villages)
  }

  /** Helper to place a specific type of settlement with spacing constraints. */
  private def placeSettlementType(
      validLocations: Seq[Point],
      count: Int,
      minSpacing: Int,
      existingSettlements: Seq[Point],
      random: Random
  ): Seq[Point] = {
    val placed = scala.collection.mutable.ArrayBuffer[Point]()
    val shuffled = random.shuffle(validLocations)
    var attempts = 0
    val maxAttempts = shuffled.size

    for (
      candidate <- shuffled if placed.size < count && attempts < maxAttempts
    ) {
      attempts += 1
      val allSettlements = existingSettlements ++ placed
      val isFarEnough = allSettlements.forall { existing =>
        val dx = candidate.x - existing.x
        val dy = candidate.y - existing.y
        Math.sqrt(dx * dx + dy * dy) >= minSpacing
      }

      if (isFarEnough) {
        placed += candidate
      }
    }

    placed.toSeq
  }

  /** Generates roads between all cities using A* pathfinding. Cities can
    * connect to other cities OR existing city roads. Optimized: maintains road
    * tile set, uses nearest-first heuristic.
    */
  private def generateRoads(
      terrain: Map[Point, OverworldTileType],
      cities: Seq[Point],
      config: OverworldMapConfig
  ): Map[Point, OverworldTileType] = {
    if (cities.size < 2) return terrain

    val mutableTerrain =
      scala.collection.mutable.Map[Point, OverworldTileType]()
    mutableTerrain ++= terrain

    // Track connected cities and maintain road tile set for efficiency
    val connectedCities = scala.collection.mutable.Set[Point](cities.head)
    val remainingCities = scala.collection.mutable.Set[Point](cities.tail*)
    val roadTileSet = scala.collection.mutable.Set[Point]()

    while (remainingCities.nonEmpty) {
      var bestPath: Option[Seq[Point]] = None
      var bestCost = Double.MaxValue
      var bestTarget: Option[Point] = None

      // For each remaining city, find path to nearest target using distance heuristic
      for (remaining <- remainingCities) {
        // Combine targets: connected cities + road tiles
        val allTargets = connectedCities.toSeq ++ roadTileSet.toSeq

        // Sort by distance and try nearest first (early termination optimization)
        val sortedTargets = allTargets
          .sortBy { t =>
            val dx = remaining.x - t.x
            val dy = remaining.y - t.y
            dx * dx + dy * dy
          }
          .take(5) // Only try 5 nearest targets

        for (target <- sortedTargets) {
          val path = findPathMutable(remaining, target, mutableTerrain, config)
          path.foreach { p =>
            val cost = p.size.toDouble
            if (cost < bestCost) {
              bestCost = cost
              bestPath = Some(p)
              bestTarget = Some(remaining)
            }
          }
        }
      }

      // Place the road and update road tile set
      bestPath.foreach { path =>
        path.foreach { point =>
          val currentTile = mutableTerrain.get(point)
          currentTile match {
            case Some(OverworldTileType.City) | Some(OverworldTileType.Town) |
                Some(OverworldTileType.Village) | Some(OverworldTileType.Road) |
                Some(OverworldTileType.Bridge) =>
            // Keep existing
            case Some(OverworldTileType.Ocean) |
                Some(OverworldTileType.Water) =>
              mutableTerrain(point) = OverworldTileType.Bridge
              roadTileSet += point
            case _ =>
              mutableTerrain(point) = OverworldTileType.Road
              roadTileSet += point
          }
        }
      }

      bestTarget.foreach { target =>
        connectedCities += target
        remainingCities -= target
      }

      if (bestPath.isEmpty) {
        remainingCities.clear()
      }
    }

    mutableTerrain.toMap
  }

  /** A* pathfinding between two points with weighted costs for terrain. */
  private def findPath(
      start: Point,
      goal: Point,
      terrain: Map[Point, OverworldTileType],
      config: OverworldMapConfig
  ): Option[Seq[Point]] = {
    import scala.collection.mutable

    case class Node(
        point: Point,
        gCost: Double,
        fCost: Double,
        parent: Option[Node]
    )

    val openSet = mutable.PriorityQueue.empty[Node](Ordering.by(-_.fCost))
    val closedSet = mutable.Set[Point]()
    val gCosts = mutable.Map[Point, Double]()

    def heuristic(p: Point): Double = {
      val dx = Math.abs(p.x - goal.x)
      val dy = Math.abs(p.y - goal.y)
      dx + dy // Manhattan distance
    }

    def movementCost(from: Point, to: Point): Double = {
      terrain.get(to) match {
        case None => Double.MaxValue // Out of bounds
        case Some(OverworldTileType.Mountain) => Double.MaxValue // Impassable
        case Some(OverworldTileType.Ocean)    =>
          15.0 // Very expensive (deep water bridge)
        case Some(OverworldTileType.Water)  => 10.0 // Expensive (water bridge)
        case Some(OverworldTileType.Forest) => 2.0 // Slightly harder
        case Some(OverworldTileType.Desert) => 1.5 // Slightly harder
        case _                              => 1.0 // Plains, Beach, settlements
      }
    }

    def neighbors(p: Point): Seq[Point] = {
      Seq(
        Point(p.x - 1, p.y),
        Point(p.x + 1, p.y),
        Point(p.x, p.y - 1),
        Point(p.x, p.y + 1)
      ).filter(n =>
        n.x >= 0 && n.x < config.width && n.y >= 0 && n.y < config.height
      )
    }

    def reconstructPath(node: Node): Seq[Point] = {
      val path = mutable.ArrayBuffer[Point]()
      var current: Option[Node] = Some(node)
      while (current.isDefined) {
        path += current.get.point
        current = current.get.parent
      }
      path.reverse.toSeq
    }

    val startNode = Node(start, 0, heuristic(start), None)
    openSet.enqueue(startNode)
    gCosts(start) = 0

    var iterations = 0
    val maxIterations =
      config.width * config.height * 2 // Prevent infinite loops

    while (openSet.nonEmpty && iterations < maxIterations) {
      iterations += 1
      val current = openSet.dequeue()

      if (current.point == goal) {
        return Some(reconstructPath(current))
      }

      if (!closedSet.contains(current.point)) {
        closedSet += current.point

        for (neighbor <- neighbors(current.point)) {
          if (!closedSet.contains(neighbor)) {
            val cost = movementCost(current.point, neighbor)
            if (cost < Double.MaxValue) {
              val newG = current.gCost + cost
              val existingG = gCosts.getOrElse(neighbor, Double.MaxValue)

              if (newG < existingG) {
                gCosts(neighbor) = newG
                val newF = newG + heuristic(neighbor)
                openSet.enqueue(Node(neighbor, newG, newF, Some(current)))
              }
            }
          }
        }
      }
    }

    None // No path found
  }

  /** Optimization: cache mutable version of findPath to avoid toMap calls. */
  private def findPathMutable(
      start: Point,
      goal: Point,
      terrain: scala.collection.mutable.Map[Point, OverworldTileType],
      config: OverworldMapConfig
  ): Option[Seq[Point]] = {
    import scala.collection.mutable

    case class Node(
        point: Point,
        gCost: Double,
        fCost: Double,
        parent: Option[Node]
    )

    val openSet = mutable.PriorityQueue.empty[Node](Ordering.by(-_.fCost))
    val closedSet = mutable.Set[Point]()
    val gCosts = mutable.Map[Point, Double]()

    def heuristic(p: Point): Double = {
      val dx = Math.abs(p.x - goal.x)
      val dy = Math.abs(p.y - goal.y)
      dx + dy // Manhattan distance
    }

    def movementCost(from: Point, to: Point): Double = {
      terrain.get(to) match {
        case None => Double.MaxValue // Out of bounds
        case Some(OverworldTileType.Mountain) => Double.MaxValue // Impassable
        case Some(OverworldTileType.Ocean)    =>
          15.0 // Very expensive (deep water bridge)
        case Some(OverworldTileType.Water)  => 10.0 // Expensive (water bridge)
        case Some(OverworldTileType.Forest) => 2.0 // Slightly harder
        case Some(OverworldTileType.Desert) => 1.5 // Slightly harder
        case _                              => 1.0 // Plains, Beach, settlements
      }
    }

    def neighbors(p: Point): Seq[Point] = {
      Seq(
        Point(p.x - 1, p.y),
        Point(p.x + 1, p.y),
        Point(p.x, p.y - 1),
        Point(p.x, p.y + 1)
      ).filter(n =>
        n.x >= 0 && n.x < config.width && n.y >= 0 && n.y < config.height
      )
    }

    def reconstructPath(node: Node): Seq[Point] = {
      val path = mutable.ArrayBuffer[Point]()
      var current: Option[Node] = Some(node)
      while (current.isDefined) {
        path += current.get.point
        current = current.get.parent
      }
      path.reverse.toSeq
    }

    val startNode = Node(start, 0, heuristic(start), None)
    openSet.enqueue(startNode)
    gCosts(start) = 0

    var iterations = 0
    val maxIterations =
      config.width * config.height * 2 // Prevent infinite loops

    while (openSet.nonEmpty && iterations < maxIterations) {
      iterations += 1
      val current = openSet.dequeue()

      if (current.point == goal) {
        return Some(reconstructPath(current))
      }

      if (!closedSet.contains(current.point)) {
        closedSet += current.point

        for (neighbor <- neighbors(current.point)) {
          if (!closedSet.contains(neighbor)) {
            val cost = movementCost(current.point, neighbor)
            if (cost < Double.MaxValue) {
              val newG = current.gCost + cost
              val existingG = gCosts.getOrElse(neighbor, Double.MaxValue)

              if (newG < existingG) {
                gCosts(neighbor) = newG
                val newF = newG + heuristic(neighbor)
                openSet.enqueue(Node(neighbor, newG, newF, Some(current)))
              }
            }
          }
        }
      }
    }

    None // No path found
  }

  /** Generates paths from towns to nearest valid connection point. Towns can
    * connect to: cities, city roads, or existing town paths. Generated
    * sequentially so new paths become valid targets.
    */
  private def generatePaths(
      terrain: Map[Point, OverworldTileType],
      towns: Seq[Point],
      config: OverworldMapConfig
  ): Map[Point, OverworldTileType] = {
    if (towns.isEmpty) return terrain

    val mutableTerrain =
      scala.collection.mutable.Map[Point, OverworldTileType]()
    mutableTerrain ++= terrain

    // Initialize cache of valid targets to avoid full map scans
    val validTargetSet = scala.collection.mutable.Set[Point]()
    mutableTerrain.foreach { case (p, t) =>
      if (
        t == OverworldTileType.City || t == OverworldTileType.Road ||
        t == OverworldTileType.Bridge
      ) {
        validTargetSet += p
      }
    }

    // Process towns sequentially so new paths become valid targets
    for (town <- towns) {
      if (validTargetSet.nonEmpty) {
        // Find nearest valid targets using cache (try top 3)
        val sortedTargets = validTargetSet.toSeq
          .sortBy { targetPoint =>
            val dx = town.x - targetPoint.x
            val dy = town.y - targetPoint.y
            dx * dx + dy * dy // Distance squared
          }
          .take(3)

        // Try to connect to nearest targets until one succeeds
        var connected = false
        val targetIterator = sortedTargets.iterator

        while (!connected && targetIterator.hasNext) {
          val target = targetIterator.next()

          // Find path from town to target
          val path = findPathMutable(town, target, mutableTerrain, config)
          path.foreach { pathPoints =>
            connected = true // Success!

            pathPoints.foreach { point =>
              val currentTile = mutableTerrain.get(point)
              // Don't overwrite settlements or existing roads/paths
              currentTile match {
                case Some(OverworldTileType.City) |
                    Some(OverworldTileType.Town) |
                    Some(OverworldTileType.Village) |
                    Some(OverworldTileType.Road) |
                    Some(OverworldTileType.Bridge) =>
                  // Keep existing, ensure it's in target set
                  validTargetSet += point
                case Some(OverworldTileType.Path) |
                    Some(OverworldTileType.PathBridge) =>
                  // Already a path, ensure in target set
                  validTargetSet += point
                case Some(OverworldTileType.Ocean) |
                    Some(OverworldTileType.Water) =>
                  mutableTerrain(point) = OverworldTileType.PathBridge
                  validTargetSet += point
                case _ =>
                  mutableTerrain(point) = OverworldTileType.Path
                  validTargetSet += point
              }
            }
          }
        }
      }
    }

    mutableTerrain.toMap
  }

  /** Generates trails from villages to nearest valid connection point. Villages
    * can connect to anything: settlements, roads, paths, or existing trails.
    * Generated sequentially so new trails become valid targets. Optimized with
    * caching.
    */
  private def generateTrails(
      terrain: Map[Point, OverworldTileType],
      villages: Seq[Point],
      config: OverworldMapConfig
  ): Map[Point, OverworldTileType] = {
    if (villages.isEmpty) return terrain

    val mutableTerrain =
      scala.collection.mutable.Map[Point, OverworldTileType]()
    mutableTerrain ++= terrain

    // Initialize cache of valid targets
    val validTargetSet = scala.collection.mutable.Set[Point]()
    mutableTerrain.foreach { case (p, t) =>
      if (
        t != OverworldTileType.Ocean && t != OverworldTileType.Water &&
        t != OverworldTileType.Plains && t != OverworldTileType.Forest &&
        t != OverworldTileType.Desert && t != OverworldTileType.Mountain &&
        t != OverworldTileType.Village // Exclude villages to prevent self-connection
      ) {
        validTargetSet += p
      }
    }

    // Process villages sequentially so new trails become valid targets
    for (village <- villages) {
      if (validTargetSet.nonEmpty) {
        // Find nearest valid targets (try top 3)
        val sortedTargets = validTargetSet.toSeq
          .filterNot(_ == village)
          .sortBy { targetPoint =>
            val dx = village.x - targetPoint.x
            val dy = village.y - targetPoint.y
            dx * dx + dy * dy // Distance squared
          }
          .take(3)

        // Try to connect to nearest targets until one succeeds
        var connected = false
        val targetIterator = sortedTargets.iterator

        while (!connected && targetIterator.hasNext) {
          val target = targetIterator.next()

          // Find path from village to target
          val path = findPathMutable(village, target, mutableTerrain, config)
          path.foreach { pathPoints =>
            connected = true // Success!

            pathPoints.foreach { point =>
              val currentTile = mutableTerrain.get(point)
              currentTile match {
                case Some(OverworldTileType.City) |
                    Some(OverworldTileType.Town) |
                    Some(OverworldTileType.Village) |
                    Some(OverworldTileType.Road) |
                    Some(OverworldTileType.Bridge) |
                    Some(OverworldTileType.Path) |
                    Some(OverworldTileType.PathBridge) |
                    Some(OverworldTileType.Trail) |
                    Some(OverworldTileType.TrailBridge) =>
                  // Keep existing, ensure in cache
                  validTargetSet += point
                case Some(OverworldTileType.Ocean) |
                    Some(OverworldTileType.Water) =>
                  mutableTerrain(point) = OverworldTileType.TrailBridge
                  validTargetSet += point
                case _ =>
                  mutableTerrain(point) = OverworldTileType.Trail
                  validTargetSet += point
              }
            }
          }
        }
      }
    }

    mutableTerrain.toMap
  }
}
