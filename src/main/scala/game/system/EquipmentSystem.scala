package game.system

import game.GameState
import game.Input
import game.entity.{Equipment, Inventory, Movement}
import game.entity.Equipment.*
import game.entity.Inventory.*
import game.entity.Movement.*
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*
import game.{Direction, Item, Point}

object EquipmentSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    events.foldLeft((gameState, Seq.empty[GameSystemEvent])) {
      case ((currentState, newEvents), EquipEvent(entityId)) =>
        currentState.getEntity(entityId) match {
          case Some(entity) =>
            val playerPosition = entity.position
            val adjacentPositions = Direction.values.map(dir => playerPosition + Direction.asPoint(dir)).toSet
            
            // Find equippable items in adjacent positions
            val adjacentEquippableItems = currentState.entities
              .filter(e => adjacentPositions.contains(e.position))
              .flatMap(_.items)
              .collect { case item: Item.EquippableItem => item }
            
            adjacentEquippableItems.headOption match {
              case Some(equipItem) =>
                // Find the entity that contains this equipment item
                val equipmentEntity = currentState.entities.find { e =>
                  adjacentPositions.contains(e.position) && e.items.contains(equipItem)
                }
                
                // Remove the item from the world and equip it
                val entityWithEquipment = if (!entity.has[Equipment]) {
                  entity.addComponent(Equipment())
                } else entity
                
                val updatedEntity = entityWithEquipment
                  .equipItem(equipItem)
                  .addItem(equipItem) // Also add to inventory for tracking
                
                val updatedState = currentState
                  .updateEntity(entityId, _ => updatedEntity)
                  .copy(messages = s"Equipped ${equipItem.name}" +: currentState.messages)
                
                // Remove the equipment entity from the world
                val finalState = equipmentEntity match {
                  case Some(eqEntity) =>
                    updatedState.remove(eqEntity.id)
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
