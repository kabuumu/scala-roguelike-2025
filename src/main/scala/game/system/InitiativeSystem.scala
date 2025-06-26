package game.system

import game.GameState
import game.entity.Initiative
import game.system.event.GameSystemEvent
import game.entity.Initiative.*

object InitiativeSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val updatedGamestate = if (gameState.playerEntity.isReady) 
      gameState
    else
      gameState.entities
        .filter(_.has[Initiative])
        .foldLeft(gameState) {
        case (currentState, entity) =>
          currentState.updateEntity(entity.id, _.decreaseInitiative())
      }
    (updatedGamestate, Nil)
  }
}
