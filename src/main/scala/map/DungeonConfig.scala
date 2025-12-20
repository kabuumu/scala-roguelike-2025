package map

import game.{Direction, Point}

/** Configuration for dungeon generation. Now simplified to take only bounds -
  * all other parameters are calculated automatically.
  *
  * @param bounds
  *   The rectangular bounds for the dungeon (defines how much space it can
  *   occupy)
  * @param seed
  *   Random seed for reproducible generation
  * @param explicitSize
  *   Optional explicit size override (for backward compatibility)
  * @param explicitLockedDoorCount
  *   Optional explicit locked door count override
  * @param explicitItemCount
  *   Optional explicit item count override
  */
case class DungeonConfig(
    bounds: MapBounds,
    seed: Long = System.currentTimeMillis(),
    explicitSize: Option[Int] = None,
    explicitLockedDoorCount: Option[Int] = None,
    explicitItemCount: Option[Int] = None,
    entranceSide: Direction = Direction.Left
) {

  /** Automatically calculate dungeon size based on available space. Uses very
    * conservative 7% of available room area for bounded dungeons. The bounded
    * generation algorithm needs significant extra space for layout constraints.
    */
  val size: Int = explicitSize.getOrElse {
    val maxRooms = bounds.roomArea
    (maxRooms * 0.3).toInt
  }

  /** Automatically calculate locked door count based on dungeon size. Roughly 1
    * locked door per 4 rooms (very conservative).
    */
  val lockedDoorCount: Int =
    explicitLockedDoorCount.getOrElse(Math.max(0, size / 6))

  /** Automatically calculate item count based on dungeon size. Roughly 1 item
    * per 6 rooms (very conservative).
    */
  val itemCount: Int = explicitItemCount.getOrElse(Math.max(1, size / 5))

  /** Validates that a room point is within the configured bounds.
    */
  def isWithinBounds(room: Point): Boolean =
    room.x >= bounds.minRoomX && room.x <= bounds.maxRoomX &&
      room.y >= bounds.minRoomY && room.y <= bounds.maxRoomY

  /** Returns a room point on the entrance side within bounds. Used for
    * positioning the dungeon entrance.
    */
  def getEntranceRoom: Point = {
    val centerX = (bounds.minRoomX + bounds.maxRoomX) / 2
    val centerY = (bounds.minRoomY + bounds.maxRoomY) / 2
    entranceSide match {
      case Direction.Up    => Point(centerX, bounds.minRoomY)
      case Direction.Down  => Point(centerX, bounds.maxRoomY)
      case Direction.Left  => Point(bounds.minRoomX, centerY)
      case Direction.Right => Point(bounds.maxRoomX, centerY)
    }
  }
}

/** Configuration for world/outdoor area generation.
  *
  * @param bounds
  *   The rectangular bounds for the world
  * @param grassDensity
  *   Percentage of tiles that should be grass (0.0 to 1.0)
  * @param treeDensity
  *   Percentage of non-grass tiles that should be trees (0.0 to 1.0)
  * @param dirtDensity
  *   Percentage of remaining tiles that should be dirt (0.0 to 1.0)
  * @param ensureWalkablePaths
  *   If true, ensures there are walkable paths through the world
  * @param perimeterTrees
  *   If true, surrounds the world with an impassable tree border
  * @param seed
  *   Random seed for reproducible generation
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
  require(
    grassDensity >= 0.0 && grassDensity <= 1.0,
    "grassDensity must be between 0.0 and 1.0"
  )
  require(
    treeDensity >= 0.0 && treeDensity <= 1.0,
    "treeDensity must be between 0.0 and 1.0"
  )
  require(
    dirtDensity >= 0.0 && dirtDensity <= 1.0,
    "dirtDensity must be between 0.0 and 1.0"
  )
  require(
    grassDensity + treeDensity + dirtDensity <= 1.0,
    "Sum of densities must not exceed 1.0"
  )
}
