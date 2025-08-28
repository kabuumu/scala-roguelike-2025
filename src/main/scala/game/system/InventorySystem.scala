package game.system

import game.entity.EntityType
import game.entity.EntityType.*
import game.entity.Inventory.*
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{CollisionTarget, GameSystemEvent}
import game.{GameState, Item}

object InventorySystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedGameState = events.collect {
      case event: GameSystemEvent.CollisionEvent => event
    }.foldLeft(gameState) {
      case (currentState, GameSystemEvent.CollisionEvent(entityId, CollisionTarget.Entity(collidedWith))) =>
        (currentState.getEntity(entityId), currentState.getEntity(collidedWith)) match {
          case (Some(entity@EntityType(Player)), Some(EntityType(ItemEntity(item)))) =>
            // Only auto-pickup non-equippable items
            item match {
              case _: Item.EquippableItem =>
                // Don't auto-pickup equippable items - they need to be equipped with Q key
                currentState
              case _ =>
                // Auto-pickup other items (potions, scrolls, arrows, etc.)
                currentState
                  .updateEntity(entityId, entity.addItem(item))
                  .remove(collidedWith)
            }
          case (Some(entity@EntityType(Player)), Some(EntityType(Key(keyColour)))) =>
            currentState
              .updateEntity(entityId, entity.addItem(Item.Key(keyColour)))
              .remove(collidedWith)
          case _ =>
            // If not an item, do nothing
            currentState
        }
    }
    (updatedGameState, Nil)
  }
}
