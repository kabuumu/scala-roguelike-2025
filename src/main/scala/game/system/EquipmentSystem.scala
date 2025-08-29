package game.system

import game.GameState
import game.Input
import game.entity.{Equipment, Inventory, Movement}
import game.entity.Equipment.*
import game.entity.Inventory.*
import game.entity.Movement.*
import game.entity.EntityType.*
import game.entity.{Equippable, CanPickUp}
import game.entity.Equippable.{isEquippable, equippable}
import game.entity.Equippable.isEquippable
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*
import game.{Direction, Point, Sprite}
import data.Sprites
import game.entity.{Entity, EntityType, EntityTypeComponent, Hitbox, Drawable}

object EquipmentSystem extends GameSystem {
  
  // Get sprite for equippable item based on its properties
  private def getEquipmentSprite(equippable: Equippable): Sprite = {
    equippable.itemName match {
      case "Leather Helmet" => Sprites.leatherHelmetSprite
      case "Iron Helmet" => Sprites.ironHelmetSprite  
      case "Chainmail Armor" => Sprites.chainmailArmorSprite
      case "Plate Armor" => Sprites.plateArmorSprite
      case _ => Sprites.defaultItemSprite
    }
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
                equipmentEntity.equippable match {
                  case Some(equippableComponent) =>
                    // Remove the item from the world and equip it
                    val entityWithEquipment = if (!entity.has[Equipment]) {
                      entity.addComponent(Equipment())
                    } else entity
                    
                    val (entityWithNewEquipment, previouslyEquippedComponent) = entityWithEquipment.equipItemComponent(equippableComponent)
                    
                    // Add the equipment to inventory
                    val updatedEntity = entityWithNewEquipment.addItemEntity(equipmentEntity.id)
                    
                    val updatedState = currentState
                      .updateEntity(entityId, _ => updatedEntity)
                      .updateEntity(equipmentEntity.id, _.removeComponent[Movement]) // Remove position instead of entity
                      .copy(messages = s"Equipped ${equippableComponent.itemName}" +: currentState.messages)
                    
                    // If there was a previously equipped item, drop it at the position where the new equipment was found
                    val finalState = previouslyEquippedComponent match {
                      case Some(droppedEquippable) =>
                        // Create a new entity for the dropped equipment
                        val dropPosition = equipmentEntity.position
                        val droppedEquipmentEntity = Entity(
                          id = s"dropped-${droppedEquippable.itemName.replace(" ", "-")}-${System.nanoTime()}",
                          Movement(position = dropPosition),
                          CanPickUp(),
                          droppedEquippable,
                          Hitbox(),
                          Drawable(getEquipmentSprite(droppedEquippable))
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
