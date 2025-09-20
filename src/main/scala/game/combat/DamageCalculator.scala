package game.combat

import game.GameState
import game.entity.Entity
import game.entity.Equipment.*
import game.status.StatusEffect
import game.status.StatusEffect.*
import game.status.StatusEffect.EffectType
import game.system.event.GameSystemEvent

final case class DamageBreakdown(
  baseDamage: Int,
  attackerBonus: Int,
  defenderResistance: Int,
  finalDamage: Int
)

object DamageCalculator {

  def compute(baseDamage: Int, attacker: Entity, defender: Entity, gameState: GameState, source: GameSystemEvent.DamageSource = GameSystemEvent.DamageSource.Melee): DamageBreakdown = {
    val statusDamageBonus = attacker.statusEffects.collect { case StatusEffect(EffectType.IncreaseDamage(b), _, _) => b }.sum
    
    // Only apply equipment damage bonus for melee attacks, not projectiles
    val equipmentDamageBonus = source match {
      case GameSystemEvent.DamageSource.Melee => attacker.getTotalDamageBonus
      case GameSystemEvent.DamageSource.Projectile => 0
    }
    
    val attackerBonus = statusDamageBonus + equipmentDamageBonus

    val statusDamageResistance = defender.statusEffects.collect { case StatusEffect(EffectType.ReduceIncomingDamage(r), _, _) => r }.sum
    val equipmentDamageResistance = defender.getTotalDamageReduction
    val defenderResistance = statusDamageResistance + equipmentDamageResistance

    val finalDamage = math.max(1, baseDamage + attackerBonus - defenderResistance)
    DamageBreakdown(baseDamage, attackerBonus, defenderResistance, finalDamage)
  }
}