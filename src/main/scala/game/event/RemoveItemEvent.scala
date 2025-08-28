package game.event

import game.Item.Item
import game.entity.Entity
import game.entity.Inventory.*

// For backward compatibility with old Item system
case class RemoveItemEvent(entityId: String, item: Item) extends EntityEvent {
  override def action: Entity => Entity = entity => {
    // Remove the first matching item entity from inventory
    val itemEntities = entity.inventoryItems(gameState = null) // TODO: Fix this properly
    // For now, just return the entity unchanged
    entity
  }
}
