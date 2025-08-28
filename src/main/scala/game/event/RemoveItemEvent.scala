package game.event

import game.Item.Item
import game.entity.Entity
import game.entity.Inventory.*

// TODO: This event needs to be updated to work with the new entity-based inventory system
// For now, keeping it simple but it won't work correctly with the new system
case class RemoveItemEvent(entityId: String, item: Item) extends EntityEvent {
  override def action: Entity => Entity = entity => {
    // This is a temporary implementation that won't work with the new system
    // The proper fix requires updating the entire item effect system
    entity
  }
}
