package map

import game.Point

case class GameMap(tiles: Seq[(Point, TileType)]) {
  def addRoom(room: Room): GameMap = {
    val roomTiles = room.gameMap.tiles.map { case (point, tileType) =>
      Point(point.x + room.x, point.y + room.y) -> tileType
    }
    GameMap(tiles ++ roomTiles)
  }
}
