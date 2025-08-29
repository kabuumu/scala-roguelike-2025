package game.entity

// Component marking that an entity is a potion with healing value
case class PotionItem(healingValue: Int = 40) extends Component

object PotionItem {
  extension (entity: Entity) {
    def potionItem: Option[PotionItem] = entity.get[PotionItem]
    def isPotion: Boolean = entity.has[PotionItem]
  }
}