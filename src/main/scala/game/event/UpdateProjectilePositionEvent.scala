package game.event

import game.entity.Entity
import game.entity.Projectile.*

case class UpdateProjectilePositionEvent(entityId: String) extends EntityEvent {
  override def action: Entity => Entity = _.updateProjectilePosition()
}
