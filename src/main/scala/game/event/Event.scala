package game.event

import game.GameState

trait Event {
  def apply(gameState: GameState): GameState
}
