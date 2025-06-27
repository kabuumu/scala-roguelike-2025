package game.system

import game.GameState
import game.entity.Health.*
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
        
        val damageResistance: Int = currentState.getEntity(entityId).toSeq.flatMap(_.statusEffects.collect {
          case StatusEffect(ReduceIncomingDamage(resistance), _, _) => resistance
        }).sum
        
        val damage = Math.max(baseDamage + damageMod - damageResistance, 0)
        
        currentState.updateEntity(entityId, _.damage(damage, attackerId))
      case (currentState, _) =>
        currentState
    } -> Nil
  }

}
