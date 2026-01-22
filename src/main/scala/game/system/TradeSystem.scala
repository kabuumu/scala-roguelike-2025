package game.system

import game.GameState
import game.entity.{Entity, Trader, Coins, Inventory, NameComponent}
import game.entity.Coins.{addCoins, removeCoins, coins}
import game.entity.Inventory.{addItemEntity, removeItemEntity}
import game.system.event.GameSystemEvent.{
  GameSystemEvent,
  InputEvent,
  SpawnEntityWithCollisionCheckEvent
}
import ui.InputAction
import data.Items.ItemReference
import scala.util.Random

object TradeSystem extends GameSystem {
  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {
    val inputEvents = events.collect { case e: InputEvent => e }

    val (updatedGameState, newEvents) =
      inputEvents.foldLeft((gameState, Seq.empty[GameSystemEvent])) {
        case ((state, currentEvents), event) =>
          event.input match {
            case InputAction.BuyItem(traderEntity, itemRef) =>
              val (newState, actionEvents) =
                handleBuyItem(state, traderEntity, itemRef)
              (newState, currentEvents ++ actionEvents)
            case InputAction.SellItem(traderEntity, itemEntity) =>
              val (newState, actionEvents) =
                handleSellItem(state, traderEntity, itemEntity)
              (newState, currentEvents ++ actionEvents)
            case _ => (state, currentEvents)
          }
      }

    (updatedGameState, newEvents)
  }

  private def handleBuyItem(
      gameState: GameState,
      trader: Entity,
      itemRef: ItemReference
  ): (GameState, Seq[GameSystemEvent]) = {
    import game.entity.Equipment.equipItemComponent
    import game.entity.Equippable.isEquippable
    import game.entity.Movement
    import game.entity.Movement.position
    import game.entity.Health._

    trader.get[Trader] match {
      case Some(traderComponent) =>
        traderComponent.buyPrice(itemRef) match {
          case Some(price) if gameState.playerEntity.coins >= price =>
            // Check for special services like healing
            if (itemRef == ItemReference.HealingService) {
              // Healing service is now handled via ConversationSystem
              (gameState, Seq.empty)
            } else {
              // Create the item
              val newItemId = s"item-${Random.nextString(8)}"
              val newItem = itemRef.createEntity(newItemId)

              // Check if the item is equippable
              if (newItem.isEquippable) {
                // Auto-equip the item
                newItem.get[game.entity.Equippable] match {
                  case Some(equippable) =>
                    val (playerWithNewEquipment, previousEquippable) =
                      gameState.playerEntity.equipItemComponent(
                        newItemId,
                        equippable
                      )
                    val updatedPlayer =
                      playerWithNewEquipment.removeCoins(price)

                    // Drop previously equipped item nearby if there was one
                    val spawnEvents = previousEquippable.map { prevEquip =>
                      val droppedItemId = s"dropped-${Random.nextString(8)}"
                      val droppedItem =
                        createEquipmentEntity(droppedItemId, prevEquip.stats)

                      val playerPos = gameState.playerEntity.position
                      val preferredPositions = Seq(
                        game.Point(playerPos.x + 1, playerPos.y),
                        game.Point(playerPos.x - 1, playerPos.y),
                        game.Point(playerPos.x, playerPos.y + 1),
                        game.Point(playerPos.x, playerPos.y - 1),
                        game.Point(playerPos.x + 1, playerPos.y + 1),
                        game.Point(playerPos.x + 1, playerPos.y - 1),
                        game.Point(playerPos.x - 1, playerPos.y + 1),
                        game.Point(playerPos.x - 1, playerPos.y - 1)
                      )

                      SpawnEntityWithCollisionCheckEvent(
                        droppedItem,
                        preferredPositions
                      )
                    }.toSeq

                    val updatedEntities = (gameState.entities
                      .filterNot(
                        _.id == gameState.playerEntity.id
                      ) :+ updatedPlayer)

                    (gameState.copy(entities = updatedEntities), spawnEvents)
                  case None => (gameState, Seq.empty)
                }
              } else {
                // Non-equippable item, just add to inventory
                val updatedPlayer = gameState.playerEntity
                  .removeCoins(price)
                  .addItemEntity(newItemId)

                val updatedEntities = gameState.entities
                  .filterNot(
                    _.id == gameState.playerEntity.id
                  ) :+ updatedPlayer :+ newItem

                (gameState.copy(entities = updatedEntities), Seq.empty)
              }
            }
          case _ => (gameState, Seq.empty) // Can't afford or item not for sale
        }
      case None => (gameState, Seq.empty)
    }
  }

  // Helper to create an equipment entity from an Equippable component
  private def createEquipmentEntity(
      id: String,
      equippable: game.entity.Equippable
  ): Entity = {
    import game.entity.{NameComponent, Drawable, Hitbox, Movement}
    import data.Sprites

    // Find the matching ItemReference to create the entity properly
    val itemRefOpt = ItemReference.values.find { ref =>
      val tempEntity = ref.createEntity("temp")
      tempEntity
        .get[game.entity.Equippable]
        .exists(_.itemName == equippable.itemName)
    }

    itemRefOpt match {
      case Some(itemRef) => itemRef.createEntity(id)
      case None          =>
        // Fallback: create a basic entity with the equippable component
        Entity(
          id = id,
          equippable,
          NameComponent(equippable.itemName, ""),
          Drawable(Sprites.defaultItemSprite),
          Hitbox(),
          Movement(position = game.Point(0, 0))
        )
    }
  }

  private def handleSellItem(
      gameState: GameState,
      trader: Entity,
      itemEntity: Entity
  ): (GameState, Seq[GameSystemEvent]) = {
    import game.entity.Equipment.{equipment, unequipItem}
    import game.entity.ItemComponent

    // Find the corresponding ItemReference using the ItemComponent
    val itemRefOpt = itemEntity.get[ItemComponent].map(_.ref)

    (trader.get[Trader], itemRefOpt) match {
      case (Some(traderComponent), Some(itemRef)) =>
        traderComponent.sellPrice(itemRef) match {
          case Some(price) =>
            // Check if item is currently equipped and unequip it first
            val (playerWithUnequipped, wasEquipped) =
              itemEntity.get[game.entity.Equippable] match {
                case Some(equippable) =>
                  // Check if this item is currently equipped
                  val currentEquipment = gameState.playerEntity.equipment
                  val isEquipped = currentEquipment
                    .getEquippedItem(equippable.slot)
                    .exists(_.stats.itemName == equippable.itemName)

                  if (isEquipped) {
                    // Unequip the item before selling
                    (gameState.playerEntity.unequipItem(equippable.slot), true)
                  } else {
                    (gameState.playerEntity, false)
                  }
                case None =>
                  (gameState.playerEntity, false)
              }

            // Remove item from player's inventory and add coins
            // For equipped items (temporary entities), they were created for display only and don't exist in game world
            // For inventory items, remove from inventory list and from entities
            val (updatedPlayer, entitiesToRemove) = if (wasEquipped) {
              // Item was equipped - we already unequipped it, just add coins
              // The temporary entity used for display doesn't exist in gameState.entities, so no need to remove it
              (playerWithUnequipped.addCoins(price), Set.empty[String])
            } else {
              // Item was in inventory - remove from inventory list and add coins
              (
                playerWithUnequipped
                  .addCoins(price)
                  .removeItemEntity(itemEntity.id),
                Set(itemEntity.id)
              )
            }

            // Remove the item entity from the world if it was in inventory
            val updatedEntities = gameState.entities
              .filterNot(e =>
                e.id == gameState.playerEntity.id || entitiesToRemove.contains(
                  e.id
                )
              ) :+ updatedPlayer

            (gameState.copy(entities = updatedEntities), Seq.empty)
          case None => (gameState, Seq.empty) // Trader doesn't buy this item
        }
      case _ => (gameState, Seq.empty)
    }
  }
}
