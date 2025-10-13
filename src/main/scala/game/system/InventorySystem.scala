package game.system

import game.entity.EntityType
import game.entity.EntityType.*
import game.entity.{Inventory, CanPickUp, Movement, NameComponent, Coins}
import game.entity.Inventory.*
import game.entity.CanPickUp.canPickUp
import game.entity.Equippable.isEquippable
import game.entity.Coins.addCoins
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{CollisionTarget, GameSystemEvent}
import game.{GameState}

object InventorySystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedGameState = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.CollisionEvent(entityId, CollisionTarget.Entity(collidedWith))) =>
        (currentState.getEntity(entityId), currentState.getEntity(collidedWith)) match {
          case (Some(entity@EntityType(Player)), Some(itemEntity)) if itemEntity.canPickUp =>
            // Check if item is a coin
            val isCoin = itemEntity.get[NameComponent].exists(_.name == "Coin")
            if (isCoin) {
              // Coins are added to the Coins component and removed from the world
              currentState
                .updateEntity(entityId, entity.addCoins(1))
                .remove(collidedWith)
            } else if (itemEntity.isEquippable) {
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
      
      case (currentState, GameSystemEvent.RemoveItemEntityEvent(playerId, itemEntityId)) =>
        currentState.getEntity(playerId) match {
          case Some(player) =>
            val updatedPlayer = player.removeItemEntity(itemEntityId)
            currentState.updateEntity(playerId, updatedPlayer)
          case None =>
            currentState
        }
      
      case (currentState, _) =>
        currentState
    }
    (updatedGameState, Nil)
  }
}
