package game.event

import game.GameState

case class RemoveEntityEvent(entityId: String) extends Event {
  override def apply(gameState: GameState): (GameState, Seq[Event]) =
    (gameState.remove(entityId), Nil)
}
