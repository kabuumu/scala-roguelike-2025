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
    val updatedGamestate = if (stateAfterResets.playerEntity.isReady) {
      stateAfterResets
    } else {
      // Check if any entity is ready
      val anyReady = stateAfterResets.entities.exists(_.isReady)

      if (anyReady) {
        // Someone is ready (likely acted this frame and is waiting for next frame, or multiple entities acting)
        // Standard progression by 1
        stateAfterResets.copy(entities = stateAfterResets.entities.map(_.decreaseInitiative()))
      } else {
        // No one is ready. We can potentially fast-forward.

        // Check for real-time entities (like projectiles) that need smooth updates
        val hasRealTimeEntities = stateAfterResets.entities.exists(_.has[game.entity.Projectile])

        val decrementAmount = if (hasRealTimeEntities) {
          1
        } else {
          // Find minimum initiative to make someone ready
          // Filter entities that have Initiative component
          val minInit = stateAfterResets.entities
            .flatMap(_.get[Initiative].map(_.currentInitiative))
            .minOption
            .getOrElse(1) // Default to 1 if no entities with initiative (shouldn't happen)

          if (minInit > 0) minInit else 1
        }

        stateAfterResets.copy(entities = stateAfterResets.entities.map(_.decreaseInitiative(decrementAmount)))
      }
    }
    
    (updatedGamestate, Nil)
  }
}
