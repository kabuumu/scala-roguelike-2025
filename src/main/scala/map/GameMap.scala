package map

import game.Point

case class GameMap(tiles: Seq[(Point, TileType)]) {
  def addRoom(location: Point, room: Room): GameMap = {
    val roomTiles = room.tiles.tiles.map { case (point, tileType) =>
      Point(point.x + location.x, point.y + location.y) -> tileType
    }
    GameMap(tiles ++ roomTiles)
  }
}
