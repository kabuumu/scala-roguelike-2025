package game.system

import game.GameState
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.MessageEvent

/**
 * System that handles message events from the GameSystemEvent architecture.
 * Replaces the deprecated MessageEvent from the old Event system.
 */
object MessageSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val updatedGameState = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.MessageEvent(message)) =>
        currentState.copy(messages = currentState.messages :+ message)
      case (currentState, _) =>
        currentState
    }
    
    (updatedGameState, Nil)
  }
}