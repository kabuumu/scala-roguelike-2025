package game.action

import game.*
import game.entity.*
import game.entity.Initiative.*

case object WaitAction extends Action {
  def apply(entity: Entity, gameState: GameState): GameState = {
    gameState.updateEntity(entity.id, entity.decreaseInitiative())
  }
}
