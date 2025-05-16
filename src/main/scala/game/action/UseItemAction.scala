package game.action

import game.entity.*
import game.entity.Initiative.*
import game.*

case class UseItemAction(effect: Entity => GameState => GameState) extends Action {
  def apply(entity: Entity, gameState: GameState): GameState =
    effect(entity)(gameState)
}
