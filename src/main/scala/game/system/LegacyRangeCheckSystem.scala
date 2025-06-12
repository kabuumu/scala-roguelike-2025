package game.system

import game.entity.{Entity, Projectile}
import game.event.MarkForDeathEvent
import game.system.event.GameSystemEvent
import game.{DeathDetails, GameState}

@deprecated
object LegacyRangeCheckSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) =
    val events = gameState.entities.collect{
      case entity @ Entity[Projectile](projectile) if projectile.isAtTarget =>
        MarkForDeathEvent(entity.id, DeathDetails(entity))
    }
    
    (gameState.handleEvents(events), Nil)
}
