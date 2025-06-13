package game

import game.action.{Action, AttackAction, MoveAction, WaitAction}
import game.entity.*
import Movement.*
import game.system.event.GameSystemEvent.{GameSystemEvent, InputEvent}
import ui.InputAction

trait EnemyAI {
  def getNextAction(enemy: Entity, gameState: GameState): GameSystemEvent
}

object EnemyAI {
  // TODO - Create more AI options in the future
  case object DefaultAI extends EnemyAI {
    def getNextAction(enemy: Entity, gameState: GameState): GameSystemEvent = {
      //TODO - Make target persistent
      val target = gameState.playerEntity
      val attackRange = enemy.get[Inventory].flatMap(_.primaryWeapon.map(_.range)).getOrElse(1) //TODO allow enemies to have secondary weapons?

      if (gameState.getVisiblePointsFor(enemy).contains(target.position)) {
        if (enemy.position.isWithinRangeOf(target.position, attackRange)) {
          InputEvent(enemy.id, InputAction.Attack(target))
        } else Pathfinder.getNextStep(enemy.position, target.position, gameState) match {
          case Some(nextStep) =>
            InputEvent(enemy.id, InputAction.Move(nextStep))
          case None =>
            InputEvent(enemy.id, InputAction.Wait)
        }
      } else {
        InputEvent(enemy.id, InputAction.Wait)
      }
    }
  }
}
