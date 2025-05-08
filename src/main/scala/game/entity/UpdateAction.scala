package game.entity

import game.entity.Health.*
import game.entity.Initiative.*
import game.{EnemyAI, GameState}

trait UpdateAction {
  def apply(entity: Entity, gameState: GameState): GameState
}

object UpdateAction {
  case object UpdateInitiative extends UpdateAction {
    override def apply(entity: Entity, gameState: GameState): GameState = {
      gameState.updateEntity(entity.id, {
        case entity if entity.isAlive && entity.notReady =>
          entity.decreaseInitiative()
        case entity =>
          entity
      })
    }
  }

  case class AIAction(ai: EnemyAI) extends UpdateAction {
    override def apply(entity: Entity, gameState: GameState): GameState = {
      if(entity.isReady)
        ai.getNextAction(entity, gameState).apply(entity, gameState)
      else
        gameState
    }
  }
}

