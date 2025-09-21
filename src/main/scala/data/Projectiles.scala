package data

import data.DeathEvents.DeathEventReference.SpawnEntity
import data.Entities.EntityReference.Explosion
import game.entity.{Collision, DeathEvents, Drawable, Entity, EntityType, EntityTypeComponent, Hitbox, Movement}

object Projectiles {
  enum ProjectileReference:
    case Arrow
    case Fireball
    case SnakeSpit
    case BossBlast
  
  def arrowProjectile(creatorId: String, startingPosition: game.Point, targetPoint: game.Point, targetType: EntityType): Entity = {
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

  def fireballProjectile(creatorId: String, startingPosition: game.Point, targetPoint: game.Point, targetType: EntityType): Entity = {
    val id = s"Projectile-${System.nanoTime()}"

    Entity(
      id = id,
      Movement(position = startingPosition),
      game.entity.Projectile(startingPosition, targetPoint),
      EntityTypeComponent(EntityType.Projectile),
      Drawable(Sprites.projectileSprite),
      Collision(damage = 0, persistent = false, targetType, creatorId),
      DeathEvents(Seq(
        SpawnEntity(Explosion(damage = 15, size = 2))
      )),
      Hitbox()
    )
  }
  
  def snakeSpitProjectile(creatorId: String, startingPosition: game.Point, targetPoint: game.Point, targetType: EntityType): Entity = {
    Entity(
      id = s"Projectile-${System.nanoTime()}",
      Movement(position = startingPosition),
      game.entity.Projectile(startingPosition, targetPoint),
      EntityTypeComponent(EntityType.Projectile),
      Drawable(Sprites.projectileSprite),
      Collision(damage = 6, persistent = false, targetType, creatorId), // Same damage as original snake weapon
      Hitbox()
    )
  }

  def bossBlastProjectile(creatorId: String, startingPosition: game.Point, targetPoint: game.Point, targetType: EntityType): Entity = {
    Entity(
      id = s"Projectile-${System.nanoTime()}",
      Movement(position = startingPosition),
      game.entity.Projectile(startingPosition, targetPoint),
      EntityTypeComponent(EntityType.Projectile),
      Drawable(Sprites.projectileSprite),
      Collision(damage = 8, persistent = false, targetType, creatorId), // Boss ranged attack - less than melee (15)
      Hitbox()
    )
  }
  
}
