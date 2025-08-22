package game.system

import game.GameState
import game.entity.Experience
import game.system.event.GameSystemEvent

object ExperienceSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val updatedGameState = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.AddExperienceEvent(entityId, amount)) =>
        currentState.updateEntity(entityId, _.update[Experience](_.addExperience(amount)))
      case (currentState, _) =>
        currentState
    }

    (updatedGameState, Nil)
  }
}
