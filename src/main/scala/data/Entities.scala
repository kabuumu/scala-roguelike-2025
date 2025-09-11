package data

import data.Sprites
import game.GameState
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Movement.position
import game.system.event.GameSystemEvent.SpawnEntityEvent

object Entities {
  enum EntityReference:
    case Arrow, Fireball

  def arrowProjectile(creatorId: String, startingPosition: game.Point, targetPoint: game.Point, targetType: EntityType) = {
    Entity(
      id = s"Projectile-${System.nanoTime()}",
      Movement(position = startingPosition),
      game.entity.Projectile(startingPosition, targetPoint),
      EntityTypeComponent(EntityType.Projectile),
      Drawable(Sprites.projectileSprite),
      Collision(damage = 8, persistent = false, targetType, creatorId),
      Hitbox()
    )
  }

  def fireballProjectile(creatorId: String, startingPosition: game.Point, targetPoint: game.Point, targetType: EntityType) = {
    val id = s"Projectile-${System.nanoTime()}"

    Entity(
      id = id,
      Movement(position = startingPosition),
      game.entity.Projectile(startingPosition, targetPoint),
      EntityTypeComponent(EntityType.Projectile),
      Drawable(Sprites.projectileSprite),
      Collision(damage = 0, persistent = false, targetType, creatorId),
      DeathEvents(deathDetails => Seq(
        SpawnEntityEvent(explosionEffect(creatorId, deathDetails.victim.position, targetType))
      )),
      Hitbox()
    )
  }

  def explosionEffect(creatorId: String, position: game.Point, targetType: EntityType) = {
    Entity(
      s"Explosion ($creatorId)",
      Hitbox(),
      Collision(damage = 12, persistent = true, targetType, creatorId),
      Movement(position = position),
      Drawable(Sprites.projectileSprite),
      Wave(2),
      EntityTypeComponent(EntityType.Projectile) // Consistent with original system
    )
  }
}
