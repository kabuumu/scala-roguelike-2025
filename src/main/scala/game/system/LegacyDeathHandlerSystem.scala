package game.system

import game.GameState
import game.entity.{DeathEvents, MarkedForDeath}
import game.event.{Event, RemoveEntityEvent}
import game.system.event.GameSystemEvent.GameSystemEvent

@deprecated
object LegacyDeathHandlerSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val newGameState = gameState.entities.foldLeft(gameState) {
      (currentGameState, entity) =>
        (entity.get[DeathEvents], entity.get[MarkedForDeath]) match {
          case (optDeathEvents, Some(markedForDeath)) =>
            // If the entity is marked for death, process the death events
            optDeathEvents match {
              case Some(deathEvents) =>
                // Apply each death event to the death details
                currentGameState.handleEvents(deathEvents.deathEvents.map(_.apply(markedForDeath.deathDetails)) :+ RemoveEntityEvent(entity.id))
              case None =>
                // If no death events are defined, just remove the entity
                currentGameState.remove(entity.id)
            }
          case _ =>
            // If the entity is not marked for death, do nothing
            currentGameState
        }
    }

    (newGameState, Nil)
  }
}
