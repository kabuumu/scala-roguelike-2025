package game.event

import game.Item.Item
import game.entity.Entity
import game.entity.Inventory.*

case class AddItemEvent(entityId: String, item: Item) extends EntityEvent {
  override def action: Entity => Entity = _.addItem(item)
}
