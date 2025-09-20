package game.combat

import game.GameState
import game.entity.Entity
import game.entity.Equipment.*
import game.status.StatusEffect
import game.status.StatusEffect.*
import game.status.StatusEffect.EffectType

final case class DamageBreakdown(
  baseDamage: Int,
  attackerBonus: Int,
  defenderResistance: Int,
  finalDamage: Int
)

object DamageCalculator {

  def compute(baseDamage: Int, attacker: Entity, defender: Entity, gameState: GameState): DamageBreakdown = {
    val attackerBonus =
      attacker.getTotalDamageBonus +
        attacker.statusEffects.collect { case StatusEffect(EffectType.IncreaseDamage(b), _, _) => b }.sum

    val defenderResistance =
      defender.getTotalDamageReduction +
        defender.statusEffects.collect { case StatusEffect(EffectType.ReduceIncomingDamage(r), _, _) => r }.sum

    val finalDamage = math.max(1, baseDamage + attackerBonus - defenderResistance)
    DamageBreakdown(baseDamage, attackerBonus, defenderResistance, finalDamage)
  }
}