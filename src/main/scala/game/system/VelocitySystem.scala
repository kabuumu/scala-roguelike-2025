package game.system

import game.GameState
import game.entity.Projectile.*
import game.entity.{Movement, Projectile}
import game.system.event.GameSystemEvent

object VelocitySystem extends GameSystem {

  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) =
    val updatedState = gameState.entities.filter(_.has[Projectile]).foldLeft(gameState) {
      case (currentState, entity) =>
        currentState.updateEntity(
          entity.id,
          _.update[Projectile](_.updatePosition())
        ).updateEntity(
          entity.id,
          entity => entity.get[Projectile] match {
            case Some(projectile) =>
              entity.update[Movement](_.copy(position = projectile.position))
            case None =>
              entity
          }
        )
        
    }

    (updatedState, Nil)
}
