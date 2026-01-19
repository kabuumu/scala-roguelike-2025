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

    val updatedEntities = gameState.entities.map { entity =>
      // Player is always active
      if (entity.id == player.id) {
        if (!entity.has[Active]) entity.addComponent(Active()) else entity
      } else {
        // Check distance
        val dist = entity.get[Movement] match {
          case Some(movement) =>
            movement.position.getChebyshevDistance(playerPos)
          case None =>
            0 // Entities without movement are always active? Or never? Let's say active for safety (e.g. inventory items if they exist as entities)
        }

        // Items in inventory likely don't have movement component, or we should check EntityType
        // For now, let's assume if it has Movement, it can be culled.
        // If it doesn't have Movement (like equipment in inventory?), we keep it active or ignore it?
        // Actually, equipment in inventory are entities? If so, they move with player?
        // Let's assume effectively: Check if it has Movement. If so, cull based on distance.
        // If no movement, keep ACTIVE (safe default).

        if (entity.has[Movement]) {
          if (dist <= ActivationRadius) {
            if (!entity.has[Active]) entity.addComponent(Active()) else entity
          } else {
            if (entity.has[Active]) entity.removeComponent[Active] else entity
          }
        } else {
          // No movement (e.g. global controllers, equipped items?), keep active
          if (!entity.has[Active]) entity.addComponent(Active()) else entity
        }
      }
    }

    (gameState.copy(entities = updatedEntities), Seq.empty)
  }
}
