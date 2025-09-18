package game.entity

// Component marking that an entity can be picked up and stored in inventory
case class CanPickUp() extends Component

object CanPickUp {
  extension (entity: Entity) {
    def canPickUp: Boolean = entity.has[CanPickUp]
  }
}