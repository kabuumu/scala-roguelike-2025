package game.event

import game.entity.Entity
import game.event.EntityEvent
import game.entity.Initiative.*

case class DecreaseInitiativeEvent(entityId: String) extends EntityEvent{
  override def action: Entity => Entity = _.decreaseInitiative()
}
