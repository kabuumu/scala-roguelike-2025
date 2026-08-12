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
      // Check if any active entity is ready
      val anyReady = stateAfterResets.activeEntities.exists(_.isReady)

      if (anyReady) {
        // Someone is ready (likely acted this frame and is waiting for next frame, or multiple entities acting)
        val activeInitIds = stateAfterResets.activeEntities.collect {
          case e if e.has[Initiative] => e.id
        }.toSet

        val updatedEntities = stateAfterResets.entities.map { e =>
          if (activeInitIds.contains(e.id)) e.decreaseInitiative(1)
          else e
        }
        stateAfterResets.copy(entities = updatedEntities)
      } else {
        // No one is ready. We can potentially fast-forward.
        val hasRealTimeEntities = stateAfterResets.activeEntities.exists(_.has[game.entity.Projectile])

        val decrementAmount = if (hasRealTimeEntities) {
          1
        } else {
          val minInit = stateAfterResets.activeEntities
            .flatMap(_.get[Initiative].map(_.currentInitiative))
            .minOption
            .getOrElse(1)

          if (minInit > 0) minInit else 1
        }

        val activeInitIds = stateAfterResets.activeEntities.collect {
          case e if e.has[Initiative] => e.id
        }.toSet

        val updatedEntities = stateAfterResets.entities.map { e =>
          if (activeInitIds.contains(e.id)) e.decreaseInitiative(decrementAmount)
          else e
        }
        stateAfterResets.copy(entities = updatedEntities)
      }
    }
    
    (updatedGamestate, Nil)
  }
}
