package game.system

import game.GameState
import game.entity.Hitbox.*
import game.entity.Movement
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.GameSystemEvent

object MovementSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val movementEvents = events.collect {
      case moveAction: GameSystemEvent.MoveAction => moveAction
    }

    val updatedGamestate = movementEvents.foldLeft(gameState) {
      case (currentState, GameSystemEvent.MoveAction(entityId, direction)) =>
        currentState.getEntity(entityId) match {
          case Some(entity) =>
            val movedEntity = entity.update[Movement](_.move(direction))

            if (movedEntity.collidesWith(gameState.movementBlockingPoints)) {
              // If the entity collides with a movement blocking point, return the current state unchanged
              currentState
            } else {
              // If the entity does not collide, update the entity in the current state
              currentState.updateEntity(entityId, movedEntity)
            }
          case None =>
            // If the entity is not found, return the current state unchanged
            currentState
        }
    }
    (updatedGamestate, Nil)
  }
}
