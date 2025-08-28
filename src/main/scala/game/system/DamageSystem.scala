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
    events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.DamageEvent(entityId, attackerId, baseDamage)) =>
        val damageMod: Int = currentState.getEntity(attackerId).toSeq.flatMap(_.statusEffects.collect {
          case StatusEffect(IncreaseDamage(damageMod), _, _) => damageMod
        }).sum
        
        val statusDamageResistance: Int = currentState.getEntity(entityId).toSeq.flatMap(_.statusEffects.collect {
          case StatusEffect(ReduceIncomingDamage(resistance), _, _) => resistance
        }).sum
        
        val equipmentDamageResistance: Int = currentState.getEntity(entityId).map(_.getTotalDamageReduction).getOrElse(0)
        
        val totalDamageResistance = statusDamageResistance + equipmentDamageResistance
        val damage = Math.max(baseDamage + damageMod - totalDamageResistance, 1)

        currentState.updateEntity(entityId, _.damage(damage, attackerId))
      case (currentState, _) =>
        currentState
    } -> Nil
  }

}
