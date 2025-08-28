package game.entity

// Component marking that an entity is a scroll with damage value
case class ScrollItem(damage: Int = 30) extends Component

object ScrollItem {
  extension (entity: Entity) {
    def scrollItem: Option[ScrollItem] = entity.get[ScrollItem]
    def isScroll: Boolean = entity.has[ScrollItem]
  }
}