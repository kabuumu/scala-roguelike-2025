package game.system

import game.GameState
import game.entity.Initiative
import game.system.event.GameSystemEvent
import game.entity.Initiative.*

object InitiativeSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val updatedGamestate = if gameState.playerEntity.isReady then gameState
    else gameState.copy(entities = gameState.entities.map(_.decreaseInitiative()))
    
    (updatedGamestate, Nil)
  }
}
