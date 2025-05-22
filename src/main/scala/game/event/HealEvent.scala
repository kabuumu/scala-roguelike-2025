package game.event

import game.entity.Entity
import game.entity.Health.*

case class HealEvent(entityId: String, healAmount: Int) extends EntityEvent {
  override def action: Entity => Entity = _.heal(healAmount)
}
