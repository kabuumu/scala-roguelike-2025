package game.system

import game.GameState
import game.Input
import game.entity.{Equipment, Inventory, Movement}
import game.entity.Equipment.*
import game.entity.Inventory.*
import game.entity.Movement.*
import game.entity.EntityType.*
import game.entity.{Equippable, CanPickUp, ItemType}
import game.entity.Equippable.isEquippable
import game.entity.ItemType.itemType
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*
import game.{Direction, Item, Point, Sprite}
import data.Sprites
import game.entity.{Entity, EntityType, EntityTypeComponent, Hitbox, Drawable}

object EquipmentSystem extends GameSystem {
  
  private def getEquipmentSprite(item: Item.EquippableItem): Sprite = {
    Sprites.itemSprites.get(item).getOrElse(Sprites.defaultItemSprite)
  }
  
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    events.foldLeft((gameState, Seq.empty[GameSystemEvent])) {
      case ((currentState, newEvents), EquipEvent(entityId)) =>
        currentState.getEntity(entityId) match {
          case Some(entity) =>
            val playerPosition = entity.position
            val adjacentPositions = Direction.values.map(dir => playerPosition + Direction.asPoint(dir)).toSet
            
            // Find equippable item entities in adjacent positions
            val adjacentEquippableEntities = currentState.entities
              .filter(e => adjacentPositions.contains(e.position))
              .filter(_.isEquippable)
            
            adjacentEquippableEntities.headOption match {
              case Some(equipmentEntity) =>
                equipmentEntity.itemType match {
                  case Some(equipItem: Item.EquippableItem) =>
                    // Remove the item from the world and equip it
                    val entityWithEquipment = if (!entity.has[Equipment]) {
                      entity.addComponent(Equipment())
                    } else entity
                    
                    val (entityWithNewEquipment, previouslyEquippedItem) = entityWithEquipment.equipItem(equipItem)
                    
                    // Add the equipment to inventory
                    val updatedEntity = entityWithNewEquipment.addItemEntity(equipmentEntity.id)
                    
                    val updatedState = currentState
                      .updateEntity(entityId, _ => updatedEntity)
                      .remove(equipmentEntity.id) // Remove the equipment entity from the world
                      .copy(messages = s"Equipped ${equipItem.name}" +: currentState.messages)
                    
                    // If there was a previously equipped item, drop it at the position where the new equipment was found
                    val finalState = previouslyEquippedItem match {
                      case Some(droppedItem) =>
                        // Create a new entity for the dropped equipment
                        val dropPosition = equipmentEntity.position
                        val droppedEquipmentEntity = Entity(
                          id = s"dropped-${droppedItem.name.replace(" ", "-")}-${System.nanoTime()}",
                          Movement(position = dropPosition),
                          EntityTypeComponent(EntityType.ItemEntity(droppedItem)),
                          ItemType(droppedItem),
                          CanPickUp(),
                          Equippable.fromEquippableItem(droppedItem),
                          Hitbox(),
                          Drawable(getEquipmentSprite(droppedItem))
                        )
                        updatedState.add(droppedEquipmentEntity)
                      case None =>
                        updatedState
                    }
                    
                    (finalState, newEvents)
                  case _ =>
                    (currentState, newEvents)
                }
              case None =>
                val messageState = currentState.copy(
                  messages = "No equippable items nearby" +: currentState.messages
                )
                (messageState, newEvents)
            }
          case None =>
            (currentState, newEvents)
        }
      case ((currentState, newEvents), _) =>
        (currentState, newEvents)
    }
  }
}
