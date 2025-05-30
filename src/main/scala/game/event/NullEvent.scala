package game.event

import game.GameState

object NullEvent extends Event {
  override def apply(gameState: GameState): (GameState, Seq[Event]) = (gameState, Nil)
}
