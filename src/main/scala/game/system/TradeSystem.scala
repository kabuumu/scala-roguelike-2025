package game.system

import game.GameState
import game.entity.{Entity, Trader, Coins, Inventory, NameComponent}
import game.entity.Coins.{addCoins, removeCoins, coins}
import game.entity.Inventory.{addItemEntity, removeItemEntity}
import game.system.event.GameSystemEvent.{GameSystemEvent, InputEvent}
import ui.InputAction
import data.Items.ItemReference
import scala.util.Random

object TradeSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val inputEvents = events.collect { case e: InputEvent => e }
    
    val updatedGameState = inputEvents.foldLeft(gameState) { (state, event) =>
      event.input match {
        case InputAction.BuyItem(traderEntity, itemRef) =>
          handleBuyItem(state, traderEntity, itemRef)
        case InputAction.SellItem(traderEntity, itemEntity) =>
          handleSellItem(state, traderEntity, itemEntity)
        case _ => state
      }
    }
    
    (updatedGameState, Seq.empty)
  }
  
  private def handleBuyItem(gameState: GameState, trader: Entity, itemRef: ItemReference): GameState = {
    import game.entity.Equipment.equipItemComponent
    import game.entity.Equippable.isEquippable
    import game.entity.Movement
    import game.entity.Movement.position
    
    trader.get[Trader] match {
      case Some(traderComponent) =>
        traderComponent.buyPrice(itemRef) match {
          case Some(price) if gameState.playerEntity.coins >= price =>
            // Create the item
            val newItemId = s"item-${Random.nextString(8)}"
            val newItem = itemRef.createEntity(newItemId)
            
            // Check if the item is equippable
            if (newItem.isEquippable) {
              // Auto-equip the item
              newItem.get[game.entity.Equippable] match {
                case Some(equippable) =>
                  val (playerWithNewEquipment, previousEquippable) = gameState.playerEntity.equipItemComponent(equippable)
                  val updatedPlayer = playerWithNewEquipment.removeCoins(price)
                  
                  // Drop previously equipped item nearby if there was one
                  val droppedItemEntities = previousEquippable.map { prevEquip =>
                    val droppedItemId = s"dropped-${Random.nextString(8)}"
                    val droppedItem = createEquipmentEntity(droppedItemId, prevEquip)
                    // Place item adjacent to player
                    val playerPos = gameState.playerEntity.position
                    val dropPos = game.Point(playerPos.x + 1, playerPos.y)
                    droppedItem.addComponent(Movement(position = dropPos))
                  }.toSeq
                  
                  val updatedEntities = (gameState.entities
                    .filterNot(_.id == gameState.playerEntity.id) :+ updatedPlayer) ++ droppedItemEntities
                  
                  gameState.copy(entities = updatedEntities)
                case None => gameState
              }
            } else {
              // Non-equippable item, just add to inventory
              val updatedPlayer = gameState.playerEntity
                .removeCoins(price)
                .addItemEntity(newItemId)
              
              val updatedEntities = gameState.entities
                .filterNot(_.id == gameState.playerEntity.id) :+ updatedPlayer :+ newItem
              
              gameState.copy(entities = updatedEntities)
            }
          case _ => gameState // Can't afford or item not for sale
        }
      case None => gameState
    }
  }
  
  // Helper to create an equipment entity from an Equippable component
  private def createEquipmentEntity(id: String, equippable: game.entity.Equippable): Entity = {
    import game.entity.{NameComponent, Drawable, Hitbox, Movement}
    import data.Sprites
    
    // Find the matching ItemReference to create the entity properly
    val itemRefOpt = ItemReference.values.find { ref =>
      val tempEntity = ref.createEntity("temp")
      tempEntity.get[game.entity.Equippable].exists(_.itemName == equippable.itemName)
    }
    
    itemRefOpt match {
      case Some(itemRef) => itemRef.createEntity(id)
      case None =>
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
  
  private def handleSellItem(gameState: GameState, trader: Entity, itemEntity: Entity): GameState = {
    // Find the corresponding ItemReference for this item
    val itemRefOpt = findItemReference(itemEntity)
    
    (trader.get[Trader], itemRefOpt) match {
      case (Some(traderComponent), Some(itemRef)) =>
        traderComponent.sellPrice(itemRef) match {
          case Some(price) =>
            // Remove item from player's inventory and add coins
            val updatedPlayer = gameState.playerEntity
              .addCoins(price)
              .removeItemEntity(itemEntity.id)
            
            val updatedEntities = gameState.entities
              .filterNot(e => e.id == gameState.playerEntity.id || e.id == itemEntity.id) :+ updatedPlayer
            
            gameState.copy(entities = updatedEntities)
          case None => gameState // Trader doesn't buy this item
        }
      case _ => gameState
    }
  }
  
  private def findItemReference(itemEntity: Entity): Option[ItemReference] = {
    itemEntity.get[NameComponent].flatMap { nameComp =>
      ItemReference.values.find { ref =>
        val refEntity = ref.createEntity("temp")
        refEntity.get[NameComponent].map(_.name) == Some(nameComp.name)
      }
    }
  }
}
