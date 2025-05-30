package game.system

import game.GameState
import game.event.Event

trait GameSystem {
  def update(gameState: GameState): Seq[Event]
}
