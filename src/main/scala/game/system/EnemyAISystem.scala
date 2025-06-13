package game.system

import game.entity.EntityType.*
import game.entity.Initiative.*
import game.entity.Movement.position
import game.entity.{EntityType, Inventory}
import game.system.event.GameSystemEvent.{GameSystemEvent, InputEvent}
import game.{GameState, Pathfinder}
import ui.InputAction

object EnemyAISystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val aiEvents = gameState.entities.collect {
      case enemy if enemy.entityType == EntityType.Enemy && enemy.isReady =>
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

    (gameState, aiEvents)
  }
}
