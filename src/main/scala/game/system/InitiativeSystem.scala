package game.system

import game.GameState
import game.entity.Initiative
import game.system.event.GameSystemEvent
import game.entity.Initiative.*

object InitiativeSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    // Handle ResetInitiativeEvent events first
    val stateAfterResets = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.ResetInitiativeEvent(entityId)) =>
        currentState.getEntity(entityId) match {
          case Some(entity) =>
            val resetEntity = entity.resetInitiative()
            currentState.updateEntity(entityId, resetEntity)
          case None =>
            currentState
        }
      case (currentState, _) =>
        currentState
    }
    
    // Then handle normal initiative progression
    val updatedGamestate = if stateAfterResets.playerEntity.isReady then stateAfterResets
    else stateAfterResets.copy(entities = stateAfterResets.entities.map(_.decreaseInitiative()))
    
    (updatedGamestate, Nil)
  }
}
