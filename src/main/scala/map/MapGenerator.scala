package map

import game.Direction._
import game.{Direction, Point}

object MapGenerator {

  def generate(mapWidth: Int, mapHeight: Int): GameMap = {
    GameMap(Seq.empty)
      .addRoom(Point(0, 0), Room(11, 8, Set(Right)))
      .addRoom(Point(10, 0), Room(11, 8, Set(Left, Down)))
      .addRoom(Point(0, 7), Room(11, 8, Set(Right)))
      .addRoom(Point(10, 7), Room(11, 8, Set(Left, Up)))
  }
}