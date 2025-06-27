package game.status

import game.entity.{Component, Entity}
import game.status.StatusEffect.*

case class StatusEffect(effect: EffectType, name: String, description: String)

case class StatusEffects(effects: Seq[StatusEffect]) extends Component

object StatusEffect {
  enum EffectType:
    case IncreaseMaxHealth(amount: Int)
    case ReduceIncomingDamage(amount: Int)
    case IncreaseDamage(amount: Int)

  extension (entity: Entity) {
    def addStatusEffect(statusEffect: StatusEffect): Entity = {
      val updatedEffects = statusEffects :+ statusEffect
      println(updatedEffects)
      entity.addComponent(StatusEffects(updatedEffects))
    }

    def removeStatusEffect(statusEffect: StatusEffect): Entity = {
      val updatedEffects = statusEffects.filterNot(_ == statusEffect)
      entity.addComponent(StatusEffects(updatedEffects))
    }

    def statusEffects: Seq[StatusEffect] = {
      entity.get[StatusEffects].map(_.effects).getOrElse(Nil)
    }
  }
}
