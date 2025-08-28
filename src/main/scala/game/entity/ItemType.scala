package game.entity

import game.Item.{EquippableItem, Item}

// Component to identify what type of item an entity represents
case class ItemType(item: Item) extends Component

object ItemType {
  extension (entity: Entity) {
    def itemType: Option[Item] = entity.get[ItemType].map(_.item)
    def isItem: Boolean = entity.has[ItemType]
  }
}