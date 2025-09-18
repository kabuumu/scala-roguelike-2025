package game.entity

import game.Point
import upickle.default.ReadWriter

case class Hitbox(points: Set[Point] = Set(Point(0, 0))) extends Component derives ReadWriter

object Hitbox {
  extension (entity: Entity) {
    def collidesWith(points: Set[Point]): Boolean =
      hitbox.nonEmpty && hitbox.intersect(points).nonEmpty

    def collidesWith(other: Entity): Boolean =
      hitbox.nonEmpty && hitbox.intersect(other.hitbox).nonEmpty

    def hitbox: Set[Point] =
      for {
        movement <- entity.get[Movement].toSet
        hitbox <- entity.get[Hitbox].toSet
        hitboxPoint <- hitbox.points
      } yield hitboxPoint + movement.position
  }
}