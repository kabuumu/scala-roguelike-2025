package game.system

import game.GameState
import game.entity.{EventMemory, EntityType, Equipment, EnemyTypeComponent}
import game.entity.EventMemory.*
import game.entity.MemoryEvent
import game.entity.Movement.position
import game.entity.EntityType.entityType
import game.entity.EnemyTypeComponent.enemyTypeName
import game.entity.Health.{currentHealth, damage}
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.GameSystemEvent
import game.combat.DamageCalculator

object EventMemorySystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val currentTime = System.nanoTime()
    
    val updatedState = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.DamageEvent(targetId, attackerId, baseDamage, source)) =>
        // Record damage taken by target
        val targetState = (currentState.getEntity(targetId), currentState.getEntity(attackerId)) match {
          case (Some(target), Some(attacker)) =>
            val breakdown = DamageCalculator.compute(baseDamage, attacker, target, currentState, source)
            
            val memoryEvent = MemoryEvent.DamageTaken(
              timestamp = currentTime,
              damage = breakdown.finalDamage,
              baseDamage = breakdown.baseDamage,
              attackerBonus = breakdown.attackerBonus,
              defenderResistance = breakdown.defenderResistance,
              source = attackerId
            )
            
            currentState.updateEntity(targetId, _.addMemoryEvent(memoryEvent))
          case _ => currentState
        }
        
        // Record damage dealt by attacker - only if both entities exist
        (targetState.getEntity(attackerId), targetState.getEntity(targetId)) match {
          case (Some(attacker), Some(target)) =>
            val breakdown = DamageCalculator.compute(baseDamage, attacker, target, targetState, source)
            
            val memoryEvent = MemoryEvent.DamageDealt(
              timestamp = currentTime,
              damage = breakdown.finalDamage,
              baseDamage = breakdown.baseDamage,
              attackerBonus = breakdown.attackerBonus,
              defenderResistance = breakdown.defenderResistance,
              target = targetId
            )
            
            targetState.updateEntity(attackerId, _.addMemoryEvent(memoryEvent))
          case _ => targetState
        }
      
      case (currentState, GameSystemEvent.InputEvent(entityId, ui.InputAction.Move(direction))) =>
        currentState.getEntity(entityId) match {
          case Some(entity) =>
            val oldPosition = entity.position
            val newPosition = oldPosition + direction
            
            val memoryEvent = MemoryEvent.MovementStep(
              timestamp = currentTime,
              direction = direction,
              fromPosition = oldPosition,
              toPosition = newPosition
            )
            
            currentState.updateEntity(entityId, _.addMemoryEvent(memoryEvent))
          case None => currentState
        }
      
      case (currentState, GameSystemEvent.RemoveItemEntityEvent(playerId, itemEntityId)) =>
        // Record item usage
        currentState.getEntity(playerId) match {
          case Some(player) =>
            currentState.getEntity(itemEntityId) match {
              case Some(itemEntity) =>
                // Get item name from NameComponent for more specific tracking
                val itemType = itemEntity.get[game.entity.NameComponent]
                  .map(_.name)
                  .getOrElse(itemEntity.entityType.toString)
                
                val memoryEvent = MemoryEvent.ItemUsed(
                  timestamp = currentTime,
                  itemType = itemType,
                  target = None // Could be enhanced to track targets if needed
                )
                
                currentState.updateEntity(playerId, _.addMemoryEvent(memoryEvent))
              case None => currentState
            }
          case None => currentState
        }
      
      case (currentState, _) =>
        currentState
    }
    
    // Check for defeated enemies (entities marked for death that were enemies)
    val stateWithDefeats = gameState.entities
      .filter(entity => entity.has[game.entity.MarkedForDeath] && entity.entityType == EntityType.Enemy)
      .foldLeft(updatedState) { (state, defeatedEnemy) =>
        defeatedEnemy.get[game.entity.MarkedForDeath] match {
          case Some(markedForDeath) =>
            markedForDeath.deathDetails.killerId match {
              case Some(killerEntityId) =>
                // Use proper enemy type component instead of sprite detection
                val enemyTypeName = defeatedEnemy.enemyTypeName
                
                val memoryEvent = MemoryEvent.EnemyDefeated(
                  timestamp = currentTime,
                  enemyType = enemyTypeName,
                  method = "combat" // Could be enhanced to track specific methods
                )
                
                state.updateEntity(killerEntityId, _.addMemoryEvent(memoryEvent))
              case None => state
            }
          case None => state
        }
      }
    
    (stateWithDefeats, Nil)
  }
}