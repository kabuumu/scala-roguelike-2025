package game.system

import game.entity.{Entity, MarkedForDeath, Projectile}
import game.system.event.GameSystemEvent
import game.{DeathDetails, GameState}

object RangeCheckSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) =
    val updatedGameState = gameState.entities.filter(_.has[Projectile]).foldLeft(gameState) {
      case (currentState, entity @ Entity[Projectile](projectile)) if projectile.isAtTarget =>
        currentState.updateEntity(entity.id, _.addComponent(MarkedForDeath(DeathDetails(entity))))
      case (currentState, _) => currentState
    }
    
    (updatedGameState, Nil)
}
