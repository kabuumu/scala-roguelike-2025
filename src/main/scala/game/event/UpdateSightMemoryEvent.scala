package game.event

import game.{GameState, Point}
import game.entity.Entity
import game.entity.SightMemory.*

case class UpdateSightMemoryEvent(entityId: String, gameState: GameState) extends EntityEvent {
  override def action: Entity => Entity = _.updateSightMemory(gameState)
}
