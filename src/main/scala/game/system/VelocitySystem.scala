package game.system

import game.GameState
import game.entity.Projectile
import game.system.event.GameSystemEvent
import game.entity.Projectile.*

object VelocitySystem extends GameSystem {

  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) =
    val updatedState = gameState.entities.filter(_.has[Projectile]).foldLeft(gameState) {
      case (currentState, entity) =>
        currentState.handleEvents(entity.projectileUpdate())
    }
    
    (updatedState, Nil)
}
