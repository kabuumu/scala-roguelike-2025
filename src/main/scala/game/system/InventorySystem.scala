package game.system

import game.entity.EntityType
import game.entity.EntityType.*
import game.entity.{Inventory, CanPickUp, Movement, NameComponent, Coins}
import game.entity.Inventory.*
import game.entity.Equipment.unequipItem
import game.entity.CanPickUp.canPickUp
import game.entity.Equippable.isEquippable
import game.entity.Coins.addCoins
import game.entity.Movement.position
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{CollisionTarget, GameSystemEvent}
import game.{GameState}

object InventorySystem extends GameSystem {
  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {
    val updatedGameState = events.foldLeft(gameState) {
      case (
            currentState,
            GameSystemEvent.CollisionEvent(
              entityId,
              CollisionTarget.Entity(collidedWith)
            )
          ) =>
        (
          currentState.getEntity(entityId),
          currentState.getEntity(collidedWith)
        ) match {
          case (Some(entity @ EntityType(Player)), Some(itemEntity))
              if itemEntity.canPickUp =>
            // Check if item is a coin
            val isCoin = itemEntity.get[NameComponent].exists(_.name == "Coin")
            if (isCoin) {
              // Coins are added to the Coins component and removed from the world
              currentState
                .updateEntity(entityId, entity.addCoins(1))
                .remove(collidedWith)
            } else {
              // Auto-pickup other items (equippable and non-equippable) - remove position instead of entire entity
              currentState
                .updateEntity(entityId, entity.addItemEntity(collidedWith))
                .updateEntity(
                  collidedWith,
                  _.removeComponent[Movement]
                ) // Remove position so it's not rendered
            }
          case (
                Some(entity @ EntityType(Player)),
                Some(EntityType(Key(keyColour)))
              ) =>
            currentState
              .updateEntity(entityId, entity.addItemEntity(collidedWith))
              .updateEntity(
                collidedWith,
                _.removeComponent[Movement]
              ) // Remove position so it's not rendered
          case _ =>
            // If not an item, do nothing
            currentState
        }

      case (
            currentState,
            GameSystemEvent.InputEvent(
              entityId,
              ui.InputAction.DropItem(itemEntityId)
            )
          ) =>
        // Handle dropping items
        (
          currentState.getEntity(entityId),
          currentState.getEntity(itemEntityId)
        ) match {
          case (Some(entity), Some(itemEntity)) =>
            // Disable equipping check - prevent dropping if equipped? OR auto-unequip?
            // "Dropping equipped items does not unequip them" -> We should unequip them.

            import game.entity.Equipment

            // Auto-unequip if equipped
            val entityAfterUnequip = entity
              .get[Equipment]
              .flatMap(_.getAllEquipped.find(_.id == itemEntityId))
              .map(equippedItem => entity.unequipItem(equippedItem.stats.slot))
              .getOrElse(entity)

            // Remove from inventory
            val updatedEntity =
              entityAfterUnequip.removeItemEntity(itemEntityId)

            // Place on map (restore Movement)
            // Check if itemEntity has Movement? (it shouldn't if in inventory)
            // We drop it at an adjacent position to avoid auto-pickup
            val playerPos = entity.position
            val dropPosition = game.Direction.values
              .map(dir => playerPos + game.Direction.asPoint(dir))
              .find(pos =>
                !currentState.worldMap.staticMovementBlockingPoints
                  .contains(pos)
              )
              .getOrElse(playerPos) // Fallback to player pos if trapped

            val updatedItemEntity =
              itemEntity.addComponent(Movement(position = dropPosition))

            currentState
              .updateEntity(entityId, _ => updatedEntity) // Update inventory
              .updateEntity(
                itemEntityId,
                _ => updatedItemEntity
              ) // Update item position
              .copy(messages =
                s"Dropped ${itemEntity.get[NameComponent].map(_.name).getOrElse("Item")}" +: currentState.messages
              )

          case _ => currentState
        }

      case (
            currentState,
            GameSystemEvent.RemoveItemEntityEvent(playerId, itemEntityId)
          ) =>
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
