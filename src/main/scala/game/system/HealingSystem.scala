package game.system

import game.GameState
import game.entity.Health.*
import game.entity.EntityType.entityType
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{HealEvent, MessageEvent}

/**
 * System that handles healing events from the GameSystemEvent architecture.
 * Replaces the deprecated HealEvent from the old Event system.
 */
object HealingSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val (updatedGameState, newEvents) = events.foldLeft((gameState, Seq.empty[GameSystemEvent.GameSystemEvent])) {
      case ((currentState, currentEvents), GameSystemEvent.HealEvent(entityId, healAmount)) =>
        currentState.getEntity(entityId) match {
          case Some(entity) =>
            if (entity.hasFullHealth) {
              // Player is already at full health, generate a message
              val messageEvent = GameSystemEvent.MessageEvent(s"${System.nanoTime()}: ${entity.entityType} is already at full health")
              (currentState, currentEvents :+ messageEvent)
            } else {
              // Heal the entity
              val healedEntity = entity.heal(healAmount)
              val updatedState = currentState.updateEntity(entityId, healedEntity)
              (updatedState, currentEvents)
            }
          case None =>
            // Entity not found, no action
            (currentState, currentEvents)
        }
      case ((currentState, currentEvents), _) =>
        // Not a heal event, pass through
        (currentState, currentEvents)
    }
    
    (updatedGameState, newEvents)
  }
}