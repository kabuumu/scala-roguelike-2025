package game.entity

import game.Point

case class Hitbox(points: Set[Point] = Set(Point(0, 0))) extends Component

object Hitbox {
  extension (entity: Entity) {
    def collidesWith(points: Set[Point]): Boolean =
      hitbox.intersect(points).nonEmpty

    def collidesWith(point: Point): Boolean =
      hitbox.contains(point)

    def collidesWith(other: Entity): Boolean =
      other.hitbox.intersect(hitbox).nonEmpty

    def hitbox: Set[Point] =
      for {
        movement <- entity.get[Movement].toSet
        hitbox <- entity.get[Hitbox].toSet
        hitboxPoint <- hitbox.points
      } yield hitboxPoint + movement.position
  }
}