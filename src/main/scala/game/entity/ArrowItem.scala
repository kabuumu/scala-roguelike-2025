package game.entity

// Component marking that an entity is an arrow (ammo)
case class ArrowItem() extends Component

object ArrowItem {
  extension (entity: Entity) {
    def arrowItem: Option[ArrowItem] = entity.get[ArrowItem]
    def isArrow: Boolean = entity.has[ArrowItem]
  }
}