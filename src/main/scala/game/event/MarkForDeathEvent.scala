package game.event

import game.entity.Entity
import game.entity.MarkedForDeath
import game.DeathDetails

case class MarkForDeathEvent(entityId: String, deathDetails: DeathDetails) extends EntityEvent {
  override def action: Entity => Entity = _.addComponent(MarkedForDeath(deathDetails))
}
