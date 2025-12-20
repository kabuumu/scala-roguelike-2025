package map

import game.Point

/** Rectangular bounds for map generation. Can be specified in room coordinates
  * or tile coordinates.
  *
  * @param minRoomX
  *   Minimum room X coordinate
  * @param maxRoomX
  *   Maximum room X coordinate
  * @param minRoomY
  *   Minimum room Y coordinate
  * @param maxRoomY
  *   Maximum room Y coordinate
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

  /** Converts room coordinates to tile coordinates.
    */
  def toTileBounds(roomSize: Int = 10): (Int, Int, Int, Int) = (
    minRoomX * roomSize,
    maxRoomX * roomSize + roomSize,
    minRoomY * roomSize,
    maxRoomY * roomSize + roomSize
  )

  /** Checks if a room point is within these bounds.
    */
  def contains(room: Point): Boolean =
    room.x >= minRoomX && room.x <= maxRoomX &&
      room.y >= minRoomY && room.y <= maxRoomY

  /** Returns a human-readable description of the bounds.
    */
  def describe: String =
    s"Bounds[rooms: ($minRoomX,$minRoomY) to ($maxRoomX,$maxRoomY), " +
      s"size: ${roomWidth}x$roomHeight rooms, area: $roomArea rooms²]"
}
