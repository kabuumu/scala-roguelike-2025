package game.event

import game.GameState

case class MessageEvent(message: String) extends Event {
  override def apply(gameState: GameState): (GameState, Seq[Event]) =
    (gameState.addMessage(message), Nil)
}
