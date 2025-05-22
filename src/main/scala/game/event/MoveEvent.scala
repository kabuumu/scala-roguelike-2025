package game.event

import game.Direction
import game.entity.{Entity, Movement}
import game.entity.Movement.*

case class MoveEvent(entityId: String, direction: Direction) extends EntityEvent {
  override def event: Entity => Entity = _.update[Movement](_.move(direction))
}
