package game

trait GameEffect {
  def apply(gameState: GameState): GameState
}

