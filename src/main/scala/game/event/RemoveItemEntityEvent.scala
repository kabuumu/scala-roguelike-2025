package game.event

import game.entity.Entity
import game.entity.Inventory.*

case class RemoveItemEntityEvent(entityId: String, itemEntityId: String) extends EntityEvent {
  override def action: Entity => Entity = entity => {
    entity.removeItemEntity(itemEntityId)
  }
}