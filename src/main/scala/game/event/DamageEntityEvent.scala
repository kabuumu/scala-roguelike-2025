package game.event

import game.entity.Entity
import game.entity.Health.*

case class DamageEntityEvent(entityId: String, damage: Int, damageDealerId: String) extends EntityEvent {
  override def action: Entity => Entity = _.damage(damage, damageDealerId)
}
