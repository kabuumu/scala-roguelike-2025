package game.system

import game.GameState
import game.entity.Collision
import game.entity.Collision.*
import game.system.GameSystem
import game.system.event.GameSystemEvent

@deprecated
object LegacyCollisionSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) =
    val updatedGamestate = gameState.entities.filter(_.has[Collision])
      .foldLeft(gameState) {
      case (currentState, entity) =>
        currentState.handleEvents(entity.collisionCheck(currentState))
      }
    
    (updatedGamestate, Nil)
}
