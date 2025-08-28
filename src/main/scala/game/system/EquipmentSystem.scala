package game.system

import game.GameState
import game.Input
import game.entity.{Equipment, Inventory, Movement}
import game.entity.Equipment.*
import game.entity.Inventory.*
import game.entity.Movement.*
import game.entity.EntityType.*
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
              .filter { entity =>
                entity.entityType match {
                  case EntityType.ItemEntity(item: Item.EquippableItem) => true
                  case _ => false
                }
              }
            
            adjacentEquippableEntities.headOption match {
              case Some(equipmentEntity) =>
                val equipItem = equipmentEntity.entityType match {
                  case EntityType.ItemEntity(equipItem: Item.EquippableItem) => equipItem
                  case _ => throw new IllegalStateException("Expected equippable item entity")
                }
                
                // Remove the item from the world and equip it
                val entityWithEquipment = if (!entity.has[Equipment]) {
                  entity.addComponent(Equipment())
                } else entity
                
                val (entityWithNewEquipment, previouslyEquippedItem) = entityWithEquipment.equipItem(equipItem)
                
                // Add the equipment to inventory with entity ID tracking
                val updatedEntity = entityWithNewEquipment.addItemWithEntityId(equipItem, equipmentEntity.id)
                
                val updatedState = currentState
                  .updateEntity(entityId, _ => updatedEntity)
                  .remove(equipmentEntity.id) // Remove the equipment entity from the world
                  .copy(messages = s"Equipped ${equipItem.name}" +: currentState.messages)
                
                // If there was a previously equipped item, drop it at the position where the new equipment was found
                val finalState = previouslyEquippedItem match {
                  case Some(droppedItem) =>
                    // Check if we have an entity ID for the previously equipped item
                    entity.getItemEntityId(droppedItem) match {
                      case Some(entityId) =>
                        // We can restore the original entity with preserved state
                        // For now, create a new entity (this could be enhanced to restore original entity)
                        val dropPosition = equipmentEntity.position
                        val droppedEquipmentEntity = Entity(
                          id = entityId, // Reuse the original entity ID to preserve state
                          Movement(position = dropPosition),
                          EntityTypeComponent(EntityType.ItemEntity(droppedItem)),
                          Hitbox(),
                          Drawable(getEquipmentSprite(droppedItem))
                        )
                        updatedState.add(droppedEquipmentEntity)
                      case None =>
                        // Create a new entity for the dropped equipment
                        val dropPosition = equipmentEntity.position
                        val droppedEquipmentEntity = Entity(
                          id = s"dropped-${droppedItem.name.replace(" ", "-")}-${System.nanoTime()}",
                          Movement(position = dropPosition),
                          EntityTypeComponent(EntityType.ItemEntity(droppedItem)),
                          Hitbox(),
                          Drawable(getEquipmentSprite(droppedItem))
                        )
                        updatedState.add(droppedEquipmentEntity)
                    }
                  case None =>
                    updatedState
                }
                
                (finalState, newEvents)
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
