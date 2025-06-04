package game.system

import game.GameState
import game.event.Event
import game.system.event.GameSystemEvent.GameSystemEvent

trait GameSystem {
  def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent])
}
