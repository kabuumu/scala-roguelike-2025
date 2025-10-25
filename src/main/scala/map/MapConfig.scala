package map

import game.{Direction, Point}

/**
 * Configuration for dungeon generation.
 * 
 * @param bounds The rectangular bounds for the dungeon (min and max room coordinates)
 * @param entranceSide The side where the dungeon entrance should be placed (North/South/East/West)
 * @param size Number of rooms to generate in the dungeon
 * @param lockedDoorCount Number of locked doors in the dungeon
 * @param itemCount Number of items to place in the dungeon
 * @param seed Random seed for reproducible generation
 */
case class DungeonConfig(
  bounds: Option[MapBounds] = None,
  entranceSide: Direction = Direction.Up,
  size: Int = 10,
  lockedDoorCount: Int = 0,
  itemCount: Int = 0,
  seed: Long = System.currentTimeMillis()
) {
  /**
   * Validates that a room point is within the configured bounds.
   * Returns true if no bounds are set (unlimited) or if point is within bounds.
   */
  def isWithinBounds(room: Point): Boolean = bounds match {
    case None => true
    case Some(b) => 
      room.x >= b.minRoomX && room.x <= b.maxRoomX &&
      room.y >= b.minRoomY && room.y <= b.maxRoomY
  }
  
  /**
   * Returns a room point on the specified entrance side within bounds.
   * Used for positioning the dungeon entrance.
   */
  def getEntranceRoom: Point = bounds match {
    case None => Point(0, 0)
    case Some(b) =>
      val centerX = (b.minRoomX + b.maxRoomX) / 2
      val centerY = (b.minRoomY + b.maxRoomY) / 2
      entranceSide match {
        case Direction.Up => Point(centerX, b.minRoomY)
        case Direction.Down => Point(centerX, b.maxRoomY)
        case Direction.Left => Point(b.minRoomX, centerY)
        case Direction.Right => Point(b.maxRoomX, centerY)
      }
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
