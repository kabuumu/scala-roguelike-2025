package game.system

import game.entity.EntityType.*
import game.entity.Initiative.*
import game.entity.Movement.position
import game.entity.{EntityType, UsableItem, UseContext}
import game.entity.Inventory.usableItems
import game.entity.Targeting.EnemyActor
import game.system.event.GameSystemEvent.{GameSystemEvent, InputEvent}
import game.GameState
import ui.InputAction
import util.Pathfinder

object EnemyAISystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val aiEvents = gameState.entities.collect {
      case enemy if enemy.entityType == EntityType.Enemy && enemy.isReady =>
        val target = gameState.playerEntity
        
        // Check for ranged abilities first
        val rangedAbilities = enemy.usableItems(gameState).filter { item =>
          item.get[UsableItem] match {
            case Some(usableItem) => usableItem.targeting match {
              case EnemyActor(range) => range > 1 // Ranged abilities have range > 1
              case _ => false
            }
            case None => false
          }
        }
        
        if (gameState.getVisiblePointsFor(enemy).contains(target.position)) {
          // Try ranged attack first if available and target is in range
          rangedAbilities.headOption match {
            case Some(rangedAbility) =>
              val usableItem = rangedAbility.get[UsableItem].get
              val range = usableItem.targeting match {
                case EnemyActor(r) => r
                case _ => 1
              }
              if (enemy.position.isWithinRangeOf(target.position, range)) {
                // Use ranged ability
                InputEvent(enemy.id, InputAction.UseItem(rangedAbility.id, usableItem, UseContext(enemy.id, Some(target))))
              } else {
                // Move closer to get in range
                Pathfinder.getNextStep(enemy.position, target.position, gameState) match {
                  case Some(nextStep) =>
                    InputEvent(enemy.id, InputAction.Move(nextStep))
                  case None =>
                    InputEvent(enemy.id, InputAction.Wait)
                }
              }
            case None =>
              // No ranged abilities, use melee
              val meleeRange = 1
              if (enemy.position.isWithinRangeOf(target.position, meleeRange)) {
                InputEvent(enemy.id, InputAction.Attack(target))
              } else {
                Pathfinder.getNextStep(enemy.position, target.position, gameState) match {
                  case Some(nextStep) =>
                    InputEvent(enemy.id, InputAction.Move(nextStep))
                  case None =>
                    InputEvent(enemy.id, InputAction.Wait)
                }
              }
          }
        } else {
          InputEvent(enemy.id, InputAction.Wait)
        }
    }

    (gameState, aiEvents)
  }
}
