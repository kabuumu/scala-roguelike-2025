package game.entity

import game.Point

case class Hitbox(points: Set[Point] = Set(Point(0, 0))) extends Component

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

    // Check if another entity is within range of this entity's hitbox
    def isWithinRangeOfHitbox(other: Entity, range: Int): Boolean = {
      other.get[Movement] match {
        case Some(otherMovement) => otherMovement.position.isWithinRangeOfAny(entity.hitbox, range)
        case None => false
      }
    }
  }
}