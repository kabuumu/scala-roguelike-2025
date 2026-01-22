package map

import game.Point
import scala.util.Random

/** Represents a village - a collection of buildings in a clustered area.
  *
  * @param buildings
  *   The buildings that make up this village (3-5 buildings)
  * @param centerLocation
  *   The approximate center of the village
  */
case class Village(
    buildings: Seq[Building],
    centerLocation: Point,
    paths: Set[Point],
    name: String,
    bounds: MapBounds
) {
  require(
    buildings.length >= 2 && buildings.length <= 5,
    "Village must have 2-5 buildings"
  )

  /** All tiles from all buildings in the village. When buildings overlap, walls
    * take precedence over floors.
    */
  lazy val tiles: Map[Point, TileType] = {
    val allTiles = buildings.flatMap(_.tiles)
    val buildingTilesMap = allTiles.groupBy(_._1).map { case (point, tiles) =>
      val tileTypes = tiles.map(_._2)
      // Prefer walls over floors
      val finalType = if (tileTypes.contains(TileType.Wall)) {
        TileType.Wall
      } else {
        tileTypes.headOption.getOrElse(
          TileType.Floor
        ) // Default to Floor if empty (shouldn't happen)
      }
      point -> finalType
    }

    // Paths should be below buildings (so building walls/floors aren't overwritten if there's minor overlap)
    // But since paths avoid buildings, it should be fine.
    // However, to be safe, we add paths first, then buildings.
    paths.map(_ -> TileType.Dirt).toMap ++ buildingTilesMap
  }

  /** All wall points from all buildings.
    */
  lazy val walls: Set[Point] = buildings.flatMap(_.walls).toSet

  /** The shop building in this village (helper for backward compatibility).
    */
  lazy val shopBuilding: Building =
    buildings.find(_.isShop).getOrElse(buildings.head)

  /** Get all entrance tiles for all buildings.
    */
  def entrances: Seq[Point] = buildings.map(_.entranceTile)
}

object Village {

  /** Generate a village at a specific location. Creates 3-5 buildings clustered
    * around a center point.
    *
    * @param centerLocation
    *   Center location in tile coordinates
    * @param seed
    *   Random seed for deterministic generation
    * @return
    *   A Village instance
    */
  def generateVillage(centerLocation: Point, seed: Long): Village = {
    val random = new Random(seed)

    // Determine number of buildings (2-5)
    val numBuildings = 2 + random.nextInt(4)

    // Mandatory building types that MUST be present in every village
    val mandatoryTypes = Seq(
      BuildingType.Farmland,
      BuildingType.Generic // Quest Giver
    )

    // Optional types to fill remaining slots
    val optionalTypes = Seq(
      BuildingType.Healer,
      BuildingType.PotionShop,
      BuildingType.EquipmentShop
    )

    // Fill the list with mandatory types first, then random optional ones
    val selectedTypes = if (numBuildings == 2) {
      mandatoryTypes
    } else {
      val remainingCount = numBuildings - mandatoryTypes.length
      val randomOptionals = Seq.fill(remainingCount)(
        optionalTypes(random.nextInt(optionalTypes.length))
      )
      mandatoryTypes ++ randomOptionals
    }

    // Shuffle the final list to randomize positions
    val buildingTypes = random.shuffle(selectedTypes)

    val buildings = (0 until numBuildings).map { i =>
      // Create varied building sizes (5-10 internal space)
      val width = 5 + random.nextInt(6) // 5-10
      val height = 5 + random.nextInt(6) // 5-10

      // Position buildings in a rough grid around center
      // Use a simple layout:
      // For 3 buildings: arrange in triangle
      // For 4 buildings: arrange in 2x2 grid
      // For 5 buildings: arrange in cross pattern
      val offset = i match {
        case 0 => Point(-15, -15) // Top-left
        case 1 => Point(5, -15) // Top-right
        case 2 => Point(-15, 5) // Bottom-left
        case 3 => Point(5, 5) // Bottom-right (if 4+ buildings)
        case 4 => Point(-5, -5) // Center (if 5 buildings)
      }

      val buildingLocation = Point(
        centerLocation.x + offset.x,
        centerLocation.y + offset.y
      )

      Building(
        location = buildingLocation,
        width = width,
        height = height,
        buildingType = buildingTypes(i)
      )
    }

    // Generate paths connecting buildings to a central point
    val paths = generateVillagePaths(buildings, centerLocation)

    // Generate a name
    val name = NameGenerator.generateName(random)

    // Calculate bounds (min/max of buildings + margin)
    val margin = 5
    val minX = buildings.map(_.location.x).min - margin
    val maxX = buildings.map(b => b.location.x + b.width).max + margin
    val minY = buildings.map(_.location.y).min - margin
    val maxY = buildings.map(b => b.location.y + b.height).max + margin

    // Convert to Room coordinates for MapBounds (approximate since MapBounds is usually rooms)
    // Actually MapBounds is just x/y/w/h concept, but often used for chunks/rooms.
    // Here we can use it for tile bounds if that's what's expected, strictly speaking MapBounds usually implies rooms?
    // Let's check usages. MapBounds in findVillageLocation converts toTileBounds.
    // So MapBounds stores "Units". If we want tile bounds, we should probably check if MapBounds can store raw tile coords.
    // Looking at finding code: bounds.toTileBounds(10) implies bounds are in Rooms.
    // If we want precise tile bounds, maybe we shouldn't use MapBounds or we should define it in "Tile" space if usage supports it.
    // However, for "is player within bounds", precise tile bounds are better.
    // Let's assume MapBounds can be used for generic bounds, but if it expects rooms (integers), we might lose precision if we convert tiles -> rooms.
    // Actually, let's look at MapBounds definition if possible. Assuming it's just min/max ints.
    // For now, I will store TILE coordinates in MapBounds and handle it carefully,
    // OR I will stick to the requested "location and size" which might just be center + radius or rect.
    // "bounds: MapBounds" was proposed. Let's use it as Tile Coordinates for the Village.
    val bounds = MapBounds(minX, maxX, minY, maxY)

    Village(buildings, centerLocation, paths, name, bounds)
  }

  object NameGenerator {
    val prefixes = Seq(
      "Oak",
      "Elm",
      "Ash",
      "Willow",
      "Stone",
      "Rock",
      "Deep",
      "High",
      "Low",
      "Green",
      "Whit",
      "Black",
      "Grim",
      "Raven",
      "Crow",
      "Eagle",
      "Wolf",
      "Bear",
      "Fox",
      "Deer"
    )
    val suffixes = Seq(
      "worth",
      "wood",
      "field",
      "haven",
      "wick",
      "by",
      "thorpe",
      "bridge",
      "ford",
      "ham",
      "ley",
      "ton",
      "bury",
      "stead",
      "hall",
      "well",
      "mere",
      "pool",
      "cliff",
      "hill"
    )

    def generateName(random: Random): String = {
      val prefix = prefixes(random.nextInt(prefixes.length))
      val suffix = suffixes(random.nextInt(suffixes.length))
      prefix + suffix
    }
  }

  /** Generates paths from all building entrances to a central hub in the
    * village. Internal helper using PathGenerator.
    */
  private def generateVillagePaths(
      buildings: Seq[Building],
      centerLocation: Point
  ): Set[Point] = {
    // Collect all building tiles to treat as obstacles
    val buildingTiles = buildings.flatMap(_.tiles.map(_._1)).toSet
    val buildingWalls = buildings.flatMap(_.walls).toSet

    // We specifically want to avoid walls, but floors are also obstacles for generic paths
    // (we don't want paths cutting through houses).
    // EXCEPT the entrance tiles themselves must be accessible.
    val entrances = buildings.map(_.entranceTile).toSet
    val obstacles = buildingTiles -- entrances

    // Find a valid hub point near the center.
    // The centerLocation might be inside a building.
    // Spiral out from center until we find a non-obstacle point.
    val hub = findNearestWalkablePoint(centerLocation, obstacles).getOrElse(
      centerLocation
    )

    // Calculate bounds for pathfinding (village area + margin)
    // MapBounds expects room coordinates (1 room = 10 tiles), so we must convert.
    val tileMinX = buildings.map(_.location.x).min - 20
    val tileMaxX = buildings.map(b => b.location.x + b.width).max + 20
    val tileMinY = buildings.map(_.location.y).min - 20
    val tileMaxY = buildings.map(b => b.location.y + b.height).max + 20

    val roomMinX = Math.floor(tileMinX / 10.0).toInt
    val roomMaxX = Math.ceil(tileMaxX / 10.0).toInt
    val roomMinY = Math.floor(tileMinY / 10.0).toInt
    val roomMaxY = Math.ceil(tileMaxY / 10.0).toInt

    val bounds = MapBounds(roomMinX, roomMaxX, roomMinY, roomMaxY)

    // Generate paths from each entrance to the hub
    entrances.flatMap { entrance =>
      // We need to ensure the entrance itself and the hub are not treated as obstacles for the pathfinder
      // But we already removed entrances from obstacles.
      // Hub also needs to be free.

      // Use PathGenerator
      PathGenerator.generatePathAroundObstacles(
        startPoint = entrance,
        targetPoint = hub,
        obstacles = obstacles,
        width = 0, // Single tile paths
        bounds = bounds
      )
    }
  }

  /** Spiral search for the nearest point that is not in the obstacles set.
    */
  private def findNearestWalkablePoint(
      center: Point,
      obstacles: Set[Point]
  ): Option[Point] = {
    val maxRadius = 20

    // Check center first
    if (!obstacles.contains(center)) return Some(center)

    // Spiral out - using lazy view to stop as soon as we find one
    val spiral = for {
      r <- (1 to maxRadius).view
      x <- -r to r
      y <- -r to r
      if Math.max(Math.abs(x), Math.abs(y)) == r
    } yield Point(center.x + x, center.y + y)

    spiral.find(p => !obstacles.contains(p))
  }

  /** Find a suitable location for a village that doesn't conflict with dungeon.
    * Similar to Shop.findShopLocation but for villages.
    *
    * @param dungeonBounds
    *   The bounds of dungeons to avoid
    * @param worldBounds
    *   The bounds of the entire world
    * @param preferredDistance
    *   Preferred distance from origin in tiles
    * @return
    *   A Point in tile coordinates suitable for village placement
    */
  def findVillageLocation(
      dungeonBounds: Seq[MapBounds],
      worldBounds: MapBounds,
      preferredDistance: Int = 30
  ): Point = {
    // Convert world bounds to tile coordinates
    val (worldMinX, worldMaxX, worldMinY, worldMaxY) =
      worldBounds.toTileBounds(10)

    // Try locations at increasing distances from origin, with more granular steps
    val distances = (30 to 150 by 20).toSeq

    val allValidCandidates = distances.flatMap { dist =>
      // Try 16 directions around a circle for better coverage
      val angles = (0 until 16).map(_ * (2 * math.Pi / 16))
      val candidates = angles.map { angle =>
        Point(
          (dist * math.cos(angle)).toInt,
          (dist * math.sin(angle)).toInt
        )
      }

      // Filter candidates that are:
      // 1. Within world bounds (with margin for village size ~40 tiles)
      // 2. Not overlapping with any dungeon bounds (with sufficient padding)
      candidates.filter { candidate =>
        val villageSize = 40 // Approximate size of village
        val withinWorld =
          candidate.x - villageSize >= worldMinX &&
            candidate.x + villageSize <= worldMaxX &&
            candidate.y - villageSize >= worldMinY &&
            candidate.y + villageSize <= worldMaxY

        val farFromAllDungeons = if (dungeonBounds.isEmpty) {
          true
        } else {
          dungeonBounds.forall { bounds =>
            val (dungeonMinX, dungeonMaxX, dungeonMinY, dungeonMaxY) =
              bounds.toTileBounds(10)
            // Use village size as padding to ensure complete separation
            val padding = villageSize + 10
            // Village bounds check: no part of the village overlaps with dungeon + padding
            val villageMinX = candidate.x - villageSize
            val villageMaxX = candidate.x + villageSize
            val villageMinY = candidate.y - villageSize
            val villageMaxY = candidate.y + villageSize

            // Check if rectangles don't overlap (with padding)
            villageMaxX < dungeonMinX - padding ||
            villageMinX > dungeonMaxX + padding ||
            villageMaxY < dungeonMinY - padding ||
            villageMinY > dungeonMaxY + padding
          }
        }

        withinWorld && farFromAllDungeons
      }
    }

    // Return first valid candidate, or try without as much padding for fallback
    allValidCandidates.headOption.getOrElse {
      // Fallback: try to place far from dungeons with less strict requirements
      val farCorners = Seq(
        Point(worldMaxX - 50, worldMaxY - 50),
        Point(worldMinX + 50, worldMaxY - 50),
        Point(worldMaxX - 50, worldMinY + 50),
        Point(worldMinX + 50, worldMinY + 50)
      )

      farCorners.headOption.getOrElse(Point(0, 0))
    }
  }
}
