package game.event

import game.GameState
import game.entity.Entity

case class AddEntityEvent(entity: Entity) extends Event {
  override def apply(gameState: GameState): (GameState, Seq[Event]) =
    (gameState.add(entity), Nil)
}
