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

object DamageSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedState = events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.DamageEvent(entityId, attackerId, baseDamage, source)) =>
        val statusDamageMod: Int = currentState.getEntity(attackerId).toSeq.flatMap(_.statusEffects.collect {
          case StatusEffect(IncreaseDamage(damageMod), _, _) => damageMod
        }).sum
        
        // Only apply equipment damage bonus for melee attacks, not projectiles
        val equipmentDamageBonus: Int = source match {
          case GameSystemEvent.DamageSource.Melee => 
            currentState.getEntity(attackerId).map(_.getTotalDamageBonus).getOrElse(0)
          case GameSystemEvent.DamageSource.Projectile => 
            0
        }
        
        val statusDamageResistance: Int = currentState.getEntity(entityId).toSeq.flatMap(_.statusEffects.collect {
          case StatusEffect(ReduceIncomingDamage(resistance), _, _) => resistance
        }).sum
        
        val equipmentDamageResistance: Int = currentState.getEntity(entityId).map(_.getTotalDamageReduction).getOrElse(0)
        
        val totalDamageBonus = statusDamageMod + equipmentDamageBonus
        val totalDamageResistance = statusDamageResistance + equipmentDamageResistance
        val damage = Math.max(baseDamage + totalDamageBonus - totalDamageResistance, 1)

        currentState.updateEntity(entityId, _.damage(damage, attackerId))
      case (currentState, _) =>
        currentState
    }
    
    (updatedState, Nil)
  }

}
