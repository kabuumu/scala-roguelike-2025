package game.entity

import upickle.default.ReadWriter

// Component marking that an entity can be picked up and stored in inventory
case class CanPickUp() extends Component derives ReadWriter

object CanPickUp {
  extension (entity: Entity) {
    def canPickUp: Boolean = entity.has[CanPickUp]
  }
}