package map

import game.Point

case class Chunk(
    coords: (Int, Int),
    tiles: Map[Point, TileType]
) {
  def getTile(point: Point): Option[TileType] = tiles.get(point)
}

object Chunk {
  val size: Int = 16 // 16x16 tiles per chunk

  def toChunkCoords(point: Point): (Int, Int) = {
    // Handling negative coordinates correctly for floor division
    val cx = if (point.x >= 0) point.x / size else (point.x - size + 1) / size
    val cy = if (point.y >= 0) point.y / size else (point.y - size + 1) / size
    (cx, cy)
  }

  def chunkBounds(chunkCoords: (Int, Int)): MapBounds = {
    val (cx, cy) = chunkCoords
    MapBounds(
      minRoomX =
        cx * size, // Here these names are slightly misleading as they are actually tile coords in this context
      maxRoomX = (cx + 1) * size - 1,
      minRoomY = cy * size,
      maxRoomY = (cy + 1) * size - 1
    )
  }
}
