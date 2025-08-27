package game.system

import game.GameState
import game.entity.{DeathEvents, MarkedForDeath}
import game.system.event.GameSystemEvent.GameSystemEvent

object DeathHandlerSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    gameState.entities
      .filter(_.has[MarkedForDeath])
      .foldLeft((gameState, Seq.empty[GameSystemEvent])) {
      case ((currentGameState, currentEvents), entity) =>
        (entity.get[DeathEvents], entity.get[MarkedForDeath]) match {
          case (optDeathEvents, Some(markedForDeath)) =>
            // If the entity is marked for death, process the death events
            optDeathEvents match {
              case Some(DeathEvents(deathEvents)) =>
                // Trigger death events and remove the entity
                (currentGameState.remove(entity.id), 
                  currentEvents ++ deathEvents(markedForDeath.deathDetails))
              case None =>
                // If no death events are defined, just remove the entity
                (currentGameState.remove(entity.id), 
                  currentEvents)
            }
          case _ =>
            // If the entity is not marked for death, do nothing
            (currentGameState, currentEvents)
        }
    }
  }
}
