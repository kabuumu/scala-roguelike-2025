package game.event

import game.GameState
import game.entity.Entity

trait EntityEvent extends Event {
  def entityId: String

  def event: Entity => Entity

  override def apply(gameState: GameState): GameState = gameState.updateEntity(entityId, event)
}

object EntityTest {
  // Player attacks an enemy with a projectile attack (bow)
  // Update initiative for player
  // Create projectile
  // OnCollision
  // Remove projectile entity
  // Damage enemy
  // If enemy is dead, create update experience for player
}