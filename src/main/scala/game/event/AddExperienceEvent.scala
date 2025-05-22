package game.event

import game.entity.Entity
import game.entity.Experience.*

case class AddExperienceEvent(entityId: String, experience: Int) extends EntityEvent {
  override def action: Entity => Entity = _.addExperience(experience)
}
