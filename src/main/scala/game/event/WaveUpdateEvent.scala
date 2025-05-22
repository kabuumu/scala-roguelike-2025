package game.event

import game.entity.{Entity, Wave}
import game.entity.Wave.*

case class WaveUpdateEvent(entityId: String) extends EntityEvent {
  override def action: Entity => Entity = entity => entity.get[Wave] match {
    case Some(wave) =>
      //TODO - Separate this into separate events?
      wave.update(entity)
    case None => 
      entity
  }
}
