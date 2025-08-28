package game.system

import game.entity.EntityType
import game.entity.EntityType.*
import game.entity.{Inventory, CanPickUp, ItemType, Movement}
import game.entity.Inventory.*
import game.entity.CanPickUp.canPickUp
import game.entity.ItemType.itemType
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
          case (Some(entity@EntityType(Player)), Some(itemEntity)) if itemEntity.canPickUp =>
            itemEntity.itemType match {
              case Some(item: Item.EquippableItem) =>
                // Don't auto-pickup equippable items - they need to be equipped with Q key
                currentState
              case Some(item) =>
                // Auto-pickup other items (non-equippable) - remove position instead of entire entity
                currentState
                  .updateEntity(entityId, entity.addItemEntity(collidedWith))
                  .updateEntity(collidedWith, _.removeComponent[Movement]) // Remove position so it's not rendered
              case None =>
                currentState
            }
          case (Some(entity@EntityType(Player)), Some(EntityType(Key(keyColour)))) =>
            currentState
              .updateEntity(entityId, entity.addItemEntity(collidedWith))
              .updateEntity(collidedWith, _.removeComponent[Movement]) // Remove position so it's not rendered
          case _ =>
            // If not an item, do nothing
            currentState
        }
    }
    (updatedGameState, Nil)
  }
}
