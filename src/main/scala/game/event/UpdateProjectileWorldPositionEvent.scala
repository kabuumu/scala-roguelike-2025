package game.event

import game.entity.{Entity, Movement, Projectile}

case class UpdateProjectileWorldPositionEvent(entityId: String) extends EntityEvent{
  override def action: Entity => Entity = entity => entity.get[Projectile] match {
    case Some(projectile) =>
      entity.update[Movement](_.copy(position = projectile.position))
    case None =>
      entity
  }
}
