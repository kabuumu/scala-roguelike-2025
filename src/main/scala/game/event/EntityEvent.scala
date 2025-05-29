package game.event

import game.GameState
import game.entity.Entity

trait EntityEvent extends Event {
  def entityId: String

  def action: Entity => Entity

  override def apply(gameState: GameState): (GameState, Seq[Event]) =
    (gameState.updateEntity(entityId, action), Nil)
}
