package game.system

import game.*
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Initiative.*
import game.entity.Movement.*
import game.entity.Equipment.*
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{GameSystemEvent}
import ui.InputAction

object AttackSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    events.foldLeft((gameState, Nil)) {
      case ((currentState, currentEvents), GameSystemEvent.InputEvent(attackingEntityId, InputAction.Attack(target))) =>
        val optAttackingEntity = currentState.getEntity(attackingEntityId)
        optAttackingEntity match {
          case Some(attackingEntity) =>
            // Melee attack: base damage 1 + equipped weapon damage bonus
            val baseDamage = 1
            val weaponDamageBonus = attackingEntity.getTotalDamageBonus
            val totalDamage = baseDamage + weaponDamageBonus
            
            val newGameState = currentState.updateEntity(attackingEntityId, _.resetInitiative())
            (newGameState, currentEvents :+ GameSystemEvent.DamageEvent(target.id, attackingEntityId, totalDamage))
          case None =>
            // Attacking entity doesn't exist - shouldn't happen, but handle gracefully
            (currentState, currentEvents)
        }
        
      case (currentState, _) =>
        currentState
    }
  }
}
