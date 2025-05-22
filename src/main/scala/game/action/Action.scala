package game.action

import game.*
import game.entity.*
import game.event.Event

trait Action {
  def apply(entity: Entity, gameState: GameState): Seq[Event]
}







