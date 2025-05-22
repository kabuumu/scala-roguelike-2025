package game.event

import game.entity.Entity
import game.entity.Projectile
import game.entity.Movement

case class UpdateProjectileWorldPositionEvent(entityId: String) extends EntityEvent{
  override def action: Entity => Entity = entity => entity.get[Projectile] match {
    case Some(projectile) =>
      entity.update[Movement](_.copy(position = projectile.position))
  }
}
