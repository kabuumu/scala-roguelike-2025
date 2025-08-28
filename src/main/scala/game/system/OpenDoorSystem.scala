package game.system

import game.GameState
import game.Item.Key
import game.entity.EntityType.LockedDoor
import game.entity.Initiative.*
import game.entity.Inventory.*
import game.entity.Movement.*
import game.entity.SightMemory.*
import game.entity.{EntityType, Movement}
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.GameSystemEvent
import ui.InputAction

object OpenDoorSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedGamestate = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.InputEvent(entityId, InputAction.Move(direction))) =>
        val updatedState = for {
          playerEntity <- currentState.getEntity(entityId)
          if playerEntity.isReady && currentState.movementBlockingPoints.contains(playerEntity.position + direction)
          (doorEntity, keyColour) <- currentState.entities.collectFirst {
            case entity@EntityType(LockedDoor(keyColour)) if entity.position == playerEntity.position + direction =>
              (entity, keyColour)
          }
          key = Key(keyColour)
          if playerEntity.keys(currentState).contains(key)
        } yield {
          // Find the key entity ID and remove it from inventory
          val keyEntityId = playerEntity.findItemEntityId(currentState, key)
          val updatedPlayer = keyEntityId match {
            case Some(entityId) => 
              playerEntity.removeItem(entityId).resetInitiative()
            case None => 
              playerEntity.resetInitiative()
          }
          
          val stateWithoutKey = keyEntityId match {
            case Some(entityId) => currentState.remove(entityId)
            case None => currentState
          }
          
          stateWithoutKey
            .updateEntity(playerEntity.id, _ => updatedPlayer)
            .remove(doorEntity.id)
        }
        
        updatedState.getOrElse(currentState)

      case (currentState, _) =>
        currentState
    }
    (updatedGamestate, Nil)
  }
}
