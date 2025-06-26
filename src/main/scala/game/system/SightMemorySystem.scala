package game.system

import game.GameState
import game.entity.SightMemory
import game.entity.SightMemory.*
import game.system.event.GameSystemEvent.GameSystemEvent

object SightMemorySystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedState = gameState.updateEntity(
      gameState.playerEntityId,
      _.updateSightMemory(gameState)
    )

    (updatedState, Nil)
  }

}
