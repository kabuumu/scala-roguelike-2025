package map

import game.{Direction, Point}

/**
 * Configuration for dungeon generation.
 * Now simplified to take only bounds - all other parameters are calculated automatically.
 * 
 * @param bounds The rectangular bounds for the dungeon (defines how much space it can occupy)
 * @param seed Random seed for reproducible generation
 * @param explicitSize Optional explicit size override (for backward compatibility)
 * @param explicitLockedDoorCount Optional explicit locked door count override
 * @param explicitItemCount Optional explicit item count override
 */
case class DungeonConfig(
  bounds: MapBounds,
  seed: Long = System.currentTimeMillis(),
  explicitSize: Option[Int] = None,
  explicitLockedDoorCount: Option[Int] = None,
  explicitItemCount: Option[Int] = None
) {
  /**
   * Automatically calculate dungeon size based on available space.
   * Uses very conservative 7% of available room area for bounded dungeons.
   * The bounded generation algorithm needs significant extra space for layout constraints.
   */
  val size: Int = explicitSize.getOrElse {
    val maxRooms = bounds.roomArea
    val targetRooms = (maxRooms * 0.07).toInt
    Math.max(5, Math.min(targetRooms, 10)) // Clamp between 5 and 10 rooms
  }
  
  /**
   * Automatically calculate locked door count based on dungeon size.
   * Roughly 1 locked door per 20 rooms (very conservative).
   */
  val lockedDoorCount: Int = explicitLockedDoorCount.getOrElse(Math.max(0, size / 20))
  
  /**
   * Automatically calculate item count based on dungeon size.
   * Roughly 1 item per 6 rooms (very conservative).
   */
  val itemCount: Int = explicitItemCount.getOrElse(Math.max(1, size / 6))
  
  /**
   * Entrance side defaults to Down for compatibility.
   */
  val entranceSide: Direction = Direction.Down
  
  /**
   * Validates that a room point is within the configured bounds.
   */
  def isWithinBounds(room: Point): Boolean = 
    room.x >= bounds.minRoomX && room.x <= bounds.maxRoomX &&
    room.y >= bounds.minRoomY && room.y <= bounds.maxRoomY
  
  /**
   * Returns a room point on the entrance side within bounds.
   * Used for positioning the dungeon entrance.
   */
  def getEntranceRoom: Point = {
    val centerX = (bounds.minRoomX + bounds.maxRoomX) / 2
    val centerY = (bounds.minRoomY + bounds.maxRoomY) / 2
    entranceSide match {
      case Direction.Up => Point(centerX, bounds.minRoomY)
      case Direction.Down => Point(centerX, bounds.maxRoomY)
      case Direction.Left => Point(bounds.minRoomX, centerY)
      case Direction.Right => Point(bounds.maxRoomX, centerY)
    }
  }
}

object DungeonConfig {
  /**
   * Creates a DungeonConfig with explicit size/items/doors for backward compatibility.
   * Bounds will be calculated to accommodate the requested size.
   */
  def withExplicitParams(
    size: Int,
    lockedDoorCount: Int = 0,
    itemCount: Int = 0,
    seed: Long = System.currentTimeMillis()
  ): DungeonConfig = {
    // Calculate bounds that can accommodate the requested size
    // Use extremely generous bounds: size * 50 area for bounded generation
    // The bounded algorithm needs much more space than unbounded due to layout constraints
    val sideLength = Math.ceil(Math.sqrt(size * 50)).toInt
    val bounds = MapBounds(-sideLength, sideLength, -sideLength, sideLength)
    
    // Create config with explicit values passed through constructor
    DungeonConfig(
      bounds = bounds,
      seed = seed,
      explicitSize = Some(size),
      explicitLockedDoorCount = Some(lockedDoorCount),
      explicitItemCount = Some(itemCount)
    )
  }
}

/**
 * Configuration for world/outdoor area generation.
 * 
 * @param bounds The rectangular bounds for the world
 * @param grassDensity Percentage of tiles that should be grass (0.0 to 1.0)
 * @param treeDensity Percentage of non-grass tiles that should be trees (0.0 to 1.0)
 * @param dirtDensity Percentage of remaining tiles that should be dirt (0.0 to 1.0)
 * @param ensureWalkablePaths If true, ensures there are walkable paths through the world
 * @param perimeterTrees If true, surrounds the world with an impassable tree border
 * @param seed Random seed for reproducible generation
 */
case class WorldConfig(
  bounds: MapBounds,
  grassDensity: Double = 0.7,
  treeDensity: Double = 0.15,
  dirtDensity: Double = 0.1,
  ensureWalkablePaths: Boolean = true,
  perimeterTrees: Boolean = true,
  seed: Long = System.currentTimeMillis()
) {
  require(grassDensity >= 0.0 && grassDensity <= 1.0, "grassDensity must be between 0.0 and 1.0")
  require(treeDensity >= 0.0 && treeDensity <= 1.0, "treeDensity must be between 0.0 and 1.0")
  require(dirtDensity >= 0.0 && dirtDensity <= 1.0, "dirtDensity must be between 0.0 and 1.0")
  require(grassDensity + treeDensity + dirtDensity <= 1.0, 
    "Sum of densities must not exceed 1.0")
}

/**
 * Rectangular bounds for map generation.
 * Can be specified in room coordinates or tile coordinates.
 * 
 * @param minRoomX Minimum room X coordinate
 * @param maxRoomX Maximum room X coordinate
 * @param minRoomY Minimum room Y coordinate
 * @param maxRoomY Maximum room Y coordinate
 */
case class MapBounds(
  minRoomX: Int,
  maxRoomX: Int,
  minRoomY: Int,
  maxRoomY: Int
) {
  require(minRoomX <= maxRoomX, "minRoomX must be <= maxRoomX")
  require(minRoomY <= maxRoomY, "minRoomY must be <= maxRoomY")
  
  /** Width in room units */
  val roomWidth: Int = maxRoomX - minRoomX + 1
  
  /** Height in room units */
  val roomHeight: Int = maxRoomY - minRoomY + 1
  
  /** Total area in room units */
  val roomArea: Int = roomWidth * roomHeight
  
  /**
   * Converts room coordinates to tile coordinates.
   */
  def toTileBounds(roomSize: Int = 10): (Int, Int, Int, Int) = (
    minRoomX * roomSize,
    maxRoomX * roomSize + roomSize,
    minRoomY * roomSize,
    maxRoomY * roomSize + roomSize
  )
  
  /**
   * Checks if a room point is within these bounds.
   */
  def contains(room: Point): Boolean = 
    room.x >= minRoomX && room.x <= maxRoomX &&
    room.y >= minRoomY && room.y <= maxRoomY
    
  /**
   * Returns a human-readable description of the bounds.
   */
  def describe: String = 
    s"Bounds[rooms: ($minRoomX,$minRoomY) to ($maxRoomX,$maxRoomY), " +
    s"size: ${roomWidth}x$roomHeight rooms, area: $roomArea rooms²]"
}

object MapBounds {
  /**
   * Creates bounds from tile coordinates.
   */
  def fromTiles(minX: Int, maxX: Int, minY: Int, maxY: Int, roomSize: Int = 10): MapBounds = {
    MapBounds(
      minX / roomSize,
      maxX / roomSize,
      minY / roomSize,
      maxY / roomSize
    )
  }
  
  /**
   * Creates centered bounds with specified dimensions.
   */
  def centered(width: Int, height: Int, centerX: Int = 0, centerY: Int = 0): MapBounds = {
    val halfWidth = width / 2
    val halfHeight = height / 2
    MapBounds(
      centerX - halfWidth,
      centerX + halfWidth - 1,
      centerY - halfHeight,
      centerY + halfHeight - 1
    )
  }
}
