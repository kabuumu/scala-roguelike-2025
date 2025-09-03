package game.event

import game.entity.Entity
import game.entity.Health.*

@deprecated("Use GameSystemEvent.HealEvent instead for better integration with game systems", "1.0")
case class HealEvent(entityId: String, healAmount: Int) extends EntityEvent {
  override def action: Entity => Entity = _.heal(healAmount)
}
