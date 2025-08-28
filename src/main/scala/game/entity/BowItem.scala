package game.entity

// Component marking that an entity is a bow with damage and ammo requirement
case class BowItem(damage: Int = 8) extends Component

object BowItem {
  extension (entity: Entity) {
    def bowItem: Option[BowItem] = entity.get[BowItem]
    def isBow: Boolean = entity.has[BowItem]
  }
}