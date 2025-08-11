package game.entity

import game.DeathDetails
import game.status.StatusEffect
import game.status.StatusEffect.*

case class Health(private val baseCurrent: Int, private val baseMax: Int) extends Component

object Health {
  def apply(max: Int): Health = new Health(max, max)

  extension (entity: Entity) {
    def healthModifier: Int = entity.statusEffects.collect {
      case StatusEffect(EffectType.IncreaseMaxHealth(amount), _, _) => amount
    }.sum
    
    def currentHealth: Int = {
      entity.get[Health].map(_.baseCurrent + healthModifier).getOrElse(0)
    }
    
    def maxHealth: Int = entity.get[Health].map(_.baseMax + healthModifier).getOrElse(0)
    
    def isAlive: Boolean = currentHealth > 0
    def isDead: Boolean = currentHealth <= 0

    def hasFullHealth: Boolean = currentHealth == maxHealth

    def damage(amount: Int, attackerId: String): Entity =
      entity.update[Health](health => health.copy(baseCurrent = Math.max(health.baseCurrent - amount, 0 - healthModifier))) match {
        case entity if entity.isDead =>
          entity.addComponent(MarkedForDeath(DeathDetails(entity, Some(attackerId))))
        case entity =>
          entity
      }
    
    def heal(amount: Int): Entity = entity.update[Health](
      health => health.copy(baseCurrent = Math.min(health.baseCurrent + amount, health.baseMax))
    )
    
  }
}
