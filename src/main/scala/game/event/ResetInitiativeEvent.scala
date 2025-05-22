package game.event

import game.entity.Entity
import game.entity.Initiative.*

case class ResetInitiativeEvent(entityId: String) extends EntityEvent {
  override def action: Entity => Entity = _.resetInitiative()
}
