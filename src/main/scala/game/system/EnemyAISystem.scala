package game.system

import game.entity.EntityType.*
import game.entity.Initiative.*
import game.entity.Movement.position
import game.entity.{Entity, EntityType, UsableItem, UseContext, Hitbox}
import game.entity.Hitbox.isWithinRangeOfHitbox
import game.entity.Inventory.usableItems
import game.entity.Targeting.EnemyActor
import game.system.event.GameSystemEvent.{GameSystemEvent, InputEvent}
import game.{GameState, Point, Direction}
import ui.InputAction
import util.Pathfinder

object EnemyAISystem extends GameSystem {
  
  // Helper function to determine if an enemy is a boss (2x2 entity)
  private def isBoss(enemy: Entity): Boolean = {
    enemy.get[Hitbox] match {
      case Some(hitbox) => hitbox.points.size > 1 // Boss has multiple hitbox points
      case None => false
    }
  }
  
  // Helper function to get entity size for pathfinding
  private def getEntitySize(enemy: Entity): Point = {
    if (isBoss(enemy)) Point(2, 2) else Point(1, 1)
  }
  
  // Boss AI: Switches between ranged and melee based on distance and strategy
  private def getBossAction(enemy: Entity, target: Entity, gameState: GameState): InputEvent = {
    val meleeRange = 1
    
    // Check if boss can attack in melee range
    if (enemy.isWithinRangeOfHitbox(target, meleeRange)) {
      // In melee range - attack!
      InputEvent(enemy.id, InputAction.Attack(target))
    } else {
      // Not in melee range - move closer using boss-sized pathfinding
      Pathfinder.getNextStepWithSize(enemy.position, target.position, gameState, Point(2, 2)) match {
        case Some(nextStep) =>
          InputEvent(enemy.id, InputAction.Move(nextStep))
        case None =>
          // Pathfinding failed - try simple directional movement as fallback
          // Try all four directions to see which ones work
          val possibleDirections = Seq(Direction.Up, Direction.Down, Direction.Left, Direction.Right)
          val targetPosition = target.position
          val bossPosition = enemy.position
          
          // Choose direction that gets us closer to target
          val bestDirection = if (Math.abs(targetPosition.x - bossPosition.x) > Math.abs(targetPosition.y - bossPosition.y)) {
            // Move horizontally
            if (targetPosition.x > bossPosition.x) Direction.Right else Direction.Left
          } else {
            // Move vertically
            if (targetPosition.y > bossPosition.y) Direction.Down else Direction.Up
          }
          
          InputEvent(enemy.id, InputAction.Move(bestDirection))
      }
    }
  }

  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val target = gameState.playerEntity
    
    // Pre-compute player's visible points once for all enemies
    // This is much faster than computing LOS for each enemy individually
    val playerVisiblePoints = gameState.getVisiblePointsFor(target)
    
    val aiEvents = gameState.entities.collect {
      case enemy if enemy.entityType == EntityType.Enemy && enemy.isReady =>
        
        // Optimization: Check if enemy is in player's line of sight instead of computing enemy's LOS
        // This is equivalent since LOS is symmetric, and we only compute player's LOS once
        val enemyPosition = enemy.position
        val enemyIsVisibleToPlayer = playerVisiblePoints.contains(enemyPosition)
        
        if (enemyIsVisibleToPlayer) {
          if (isBoss(enemy)) {
            // Use boss-specific AI
            getBossAction(enemy, target, gameState)
          } else {
            // Use regular enemy AI
            val rangedAbilities = enemy.usableItems(gameState).filter { item =>
              item.get[UsableItem] match {
                case Some(usableItem) => usableItem.targeting match {
                  case EnemyActor(range) => range > 1 // Ranged abilities have range > 1
                  case _ => false
                }
                case None => false
              }
            }
            
            // Try ranged attack first if available and target is in range
            rangedAbilities.headOption match {
              case Some(rangedAbility) =>
                val usableItem = rangedAbility.get[UsableItem].get
                val range = usableItem.targeting match {
                  case EnemyActor(r) => r
                  case _ => 1
                }
                if (enemy.isWithinRangeOfHitbox(target, range)) {
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
                if (enemy.isWithinRangeOfHitbox(target, meleeRange)) {
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
          }
        } else {
          InputEvent(enemy.id, InputAction.Wait)
        }
    }

    (gameState, aiEvents)
  }
}
