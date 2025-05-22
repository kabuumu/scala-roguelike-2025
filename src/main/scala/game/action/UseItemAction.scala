package game.action

import game.entity.*
import game.entity.Initiative.*
import game.*
import game.event.Event

case class UseItemAction(effect: Entity => GameState => Seq[Event]) extends Action {
  def apply(entity: Entity, gameState: GameState): Seq[Event] =
    effect(entity)(gameState)
}
