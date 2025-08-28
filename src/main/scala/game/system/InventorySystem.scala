package game.system

import game.entity.EntityType
import game.entity.EntityType.*
import game.entity.{Inventory, CanPickUp, Movement}
import game.entity.Inventory.*
import game.entity.CanPickUp.canPickUp
import game.entity.Equippable.isEquippable
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
            // Check if item is equippable (should not be auto-picked up)
            if (itemEntity.isEquippable) {
              // Don't auto-pickup equippable items - they need to be equipped with Q key
              currentState
            } else {
              // Auto-pickup other items (non-equippable) - remove position instead of entire entity
              currentState
                .updateEntity(entityId, entity.addItemEntity(collidedWith))
                .updateEntity(collidedWith, _.removeComponent[Movement]) // Remove position so it's not rendered
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
