package game.system

import game.GameState
import game.Item.Key
import game.entity.EntityType.LockedDoor
import game.entity.Initiative.*
import game.entity.Inventory.*
import game.entity.Movement.*
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
          if playerEntity.items.contains(key)
        } yield currentState
          .updateEntity(
            playerEntity.id,
            _.removeItem(key)
              .resetInitiative()
          ).remove(doorEntity.id)
        
        updatedState.getOrElse(currentState)

      case (currentState, _) =>
        currentState
    }
    (updatedGamestate, Nil)
  }
}
