package game.system

import game.GameState
import game.entity.Movement
import game.entity.Movement.*
import game.entity.SightMemory.*
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.GameSystemEvent
import game.entity.Initiative.*
import ui.InputAction

object MovementSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedGamestate = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.InputEvent(entityId, InputAction.Move(direction))) =>
        currentState.getEntity(entityId) match {
          case Some(entity) if entity.isReady && !gameState.movementBlockingPoints.contains(entity.position + direction) =>
            currentState
              .updateEntity(
                entityId,
                _.update[Movement](_.move(direction))
                  .updateSightMemory(
                    currentState
                  )
                  .resetInitiative()
              )
          case _ =>
            currentState
        }
      case (currentState, _) =>
        currentState
    }
    (updatedGamestate, Nil)
  }
}
