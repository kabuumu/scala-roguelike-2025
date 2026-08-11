package game.system

import game.GameState
import game.system.event.GameSystemEvent.GameSystemEvent
import game.entity.{Active, Entity, Movement}
import game.entity.Movement.position

object CullingSystem extends GameSystem {

  // Entities within this range of the player are active
  private val ActivationRadius =
    25 // Slightly larger than screen/loading radius

  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {
    val player = gameState.playerEntity
    val playerPos = player.position

    var modified = false
    val updatedEntities = gameState.entities.map { entity =>
      // Player is always active
      if (entity.id == player.id) {
        if (!entity.has[Active]) {
          modified = true
          entity.addComponent(Active())
        } else entity
      } else {
        if (entity.has[Movement]) {
          val dist = entity.get[Movement].map(_.position.getChebyshevDistance(playerPos)).getOrElse(0.0)
          if (dist <= ActivationRadius) {
            if (!entity.has[Active]) {
              modified = true
              entity.addComponent(Active())
            } else entity
          } else {
            if (entity.has[Active]) {
              modified = true
              entity.removeComponent[Active]
            } else entity
          }
        } else {
          // No movement (e.g. global controllers, equipped items?), keep active
          if (!entity.has[Active]) {
            modified = true
            entity.addComponent(Active())
          } else entity
        }
      }
    }

    val finalState = if (modified) gameState.copy(entities = updatedEntities) else gameState
    (finalState, Seq.empty)
  }
}
