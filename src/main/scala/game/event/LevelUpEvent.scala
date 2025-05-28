package game.event

import game.entity.Entity
import game.entity.Experience.*

case class LevelUpEvent(entityId: String) extends EntityEvent {
  override def action: Entity => Entity = _.levelUp
}
