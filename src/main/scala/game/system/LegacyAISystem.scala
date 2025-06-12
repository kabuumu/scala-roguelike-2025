package game.system

import game.EnemyAI.DefaultAI
import game.entity.Initiative.*
import game.system.event.GameSystemEvent.GameSystemEvent
import game.{EnemyAI, GameState}
import game.entity.EntityType.*
import game.entity.EntityType

@deprecated
object LegacyAISystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedGamestate = gameState.entities.filter(_.entityType == EntityType.Enemy).foldLeft(gameState) {
      case (currentState, entity) if entity.isReady =>
        val events = DefaultAI.getNextAction(entity, currentState).apply(entity, currentState)
        currentState.handleEvents(events)
      case (currentState, entity) =>
        currentState
    }

    (updatedGamestate, Nil)
  }
}
