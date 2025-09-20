package game.system

import game.GameState
import game.entity.Health.*
import game.entity.Equipment
import game.entity.Equipment.*
import game.system.event.GameSystemEvent.*
import game.system.event.GameSystemEvent
import game.status.StatusEffect.*
import game.status.StatusEffect
import game.status.StatusEffect.EffectType.{IncreaseDamage, ReduceIncomingDamage}
import game.combat.DamageCalculator

object DamageSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedState = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.DamageEvent(entityId, attackerId, baseDamage, source)) =>
        (currentState.getEntity(attackerId), currentState.getEntity(entityId)) match {
          case (Some(attacker), Some(defender)) =>
            val breakdown = DamageCalculator.compute(baseDamage, attacker, defender, currentState, source)
            currentState.updateEntity(entityId, _.damage(breakdown.finalDamage, attackerId))
          case _ =>
            // If either entity doesn't exist, no damage is applied
            currentState
        }
      case (currentState, _) =>
        currentState
    }
    
    (updatedState, Nil)
  }

}
