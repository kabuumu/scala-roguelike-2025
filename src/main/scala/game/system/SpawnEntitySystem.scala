package game.system


import game.GameState
import game.entity.SightMemory
import game.entity.SightMemory.*
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*

object SpawnEntitySystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedGamestate = events.foldLeft(gameState) {
      case (currentState, SpawnEntityEvent(entity)) =>
        currentState.add(entity)
      case (currentState, _) =>
        currentState
    }

    (updatedGamestate, Nil)
  }
}
