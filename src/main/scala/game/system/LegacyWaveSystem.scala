package game.system

import game.GameState
import game.entity.Wave
import game.system.GameSystem
import game.system.event.GameSystemEvent
import game.entity.Wave.*

@deprecated
object LegacyWaveSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) =
    val updatedGamestate = gameState.entities.filter(_.has[Wave])
      .foldLeft(gameState) {
      case (currentState, entity) =>
        currentState.handleEvents(entity.waveUpdate(currentState))
      }
    
    (updatedGamestate, Nil)
}
