package game.perk

import game.entity.{Component, Entity}
import game.status.StatusEffect
import game.status.StatusEffect.EffectType.IncreaseMaxHealth

object Perks {
  final val IncreaseMaxHealthPerk = StatusEffect(
    IncreaseMaxHealth(20),
    name = "Hearty",
    description = "Increases maximum health by 20."
  )
  
  final val ReduceIncomingDamagePerk = StatusEffect(
    StatusEffect.EffectType.ReduceIncomingDamage(5),
    name = "Fortified",
    description = "Reduces incoming damage by 5."
  )
  
  final val IncreaseDamagePerk = StatusEffect(
    StatusEffect.EffectType.IncreaseDamage(5),
    name = "Berserker",
    description = "Increases damage dealt by 5."
  )
}
