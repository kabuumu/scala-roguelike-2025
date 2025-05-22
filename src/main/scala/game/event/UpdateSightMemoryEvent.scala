package game.event

import game.Point
import game.entity.Entity

case class UpdateSightMemoryEvent(entityId: String, newVisiblePoints: Seq[Point]) extends EntityEvent {

  override def event: Entity => Entity = ???
}
