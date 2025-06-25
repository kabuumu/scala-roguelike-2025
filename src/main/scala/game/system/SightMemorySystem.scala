package game.system

import game.GameState
import game.entity.SightMemory
import game.entity.SightMemory.*
import game.system.event.GameSystemEvent.GameSystemEvent

object SightMemorySystem extends GameSystem {
  // SightMemorySystem is a placeholder for future implementation
  // Currently, it does not have any specific functionality or methods.
  // It can be expanded later to handle sight memory related logic.

  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedState = gameState.entities.filter(_.has[SightMemory]).foldLeft(gameState) {
      case (currentState, entity) =>
        currentState.updateEntity(
          entity.id,
          _.updateSightMemory(currentState)
        )
    }

    (updatedState, Nil)
  }

}
