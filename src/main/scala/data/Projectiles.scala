package data

import data.DeathEvents.DeathEventReference.SpawnEntity
import data.Entities.EntityReference.Explosion
import game.entity.{
  Collision,
  DeathEvents,
  Drawable,
  Entity,
  EntityType,
  EntityTypeComponent,
  Hitbox,
  Movement
}

object Projectiles {
  enum ProjectileReference:
    case Arrow
    case Fireball
    case SnakeSpit
    case BossBlast
    case LightningBolt(
        damage: Int,
        bounces: Int,
        bounceRange: Int,
        overrideTargetType: Option[game.entity.EntityType] = None
    )

  def arrowProjectile(
      creatorId: String,
      startingPosition: game.Point,
      targetPoint: game.Point,
      targetType: EntityType
  ): Entity = {
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

  def fireballProjectile(
      creatorId: String,
      startingPosition: game.Point,
      targetPoint: game.Point,
      targetType: EntityType
  ): Entity = {
    val id = s"Projectile-${System.nanoTime()}"

    Entity(
      id = id,
      Movement(position = startingPosition),
      game.entity.Projectile(startingPosition, targetPoint),
      EntityTypeComponent(EntityType.Projectile),
      Drawable(Sprites.projectileSprite),
      Collision(damage = 0, persistent = false, targetType, creatorId),
      DeathEvents(
        Seq(
          SpawnEntity(Explosion(damage = 15, size = 2))
        )
      ),
      Hitbox()
    )
  }

  def snakeSpitProjectile(
      creatorId: String,
      startingPosition: game.Point,
      targetPoint: game.Point,
      targetType: EntityType
  ): Entity = {
    Entity(
      id = s"Projectile-${System.nanoTime()}",
      Movement(position = startingPosition),
      game.entity.Projectile(startingPosition, targetPoint),
      EntityTypeComponent(EntityType.Projectile),
      Drawable(Sprites.projectileSprite),
      Collision(
        damage = 6,
        persistent = false,
        targetType,
        creatorId
      ), // Same damage as original snake weapon
      Hitbox()
    )
  }

  def bossBlastProjectile(
      creatorId: String,
      startingPosition: game.Point,
      targetPoint: game.Point,
      targetType: EntityType
  ): Entity = {
    Entity(
      id = s"Projectile-${System.nanoTime()}",
      Movement(position = startingPosition),
      game.entity.Projectile(startingPosition, targetPoint),
      EntityTypeComponent(EntityType.Projectile),
      Drawable(Sprites.projectileSprite),
      Collision(
        damage = 8,
        persistent = false,
        targetType,
        creatorId
      ), // Boss ranged attack - less than melee (15)
      Hitbox()
    )
  }

  def lightningBoltProjectile(
      creatorId: String,
      startingPosition: game.Point,
      targetPoint: game.Point,
      targetType: EntityType,
      damage: Int,
      bounces: Int,
      bounceRange: Int
  ): Entity = {
    val entity = Entity(
      id = s"Projectile-${System.nanoTime()}",
      Movement(position = startingPosition),
      game.entity.Projectile(startingPosition, targetPoint),
      EntityTypeComponent(EntityType.Projectile),
      Drawable(Sprites.projectileSprite),
      Collision(
        damage = damage, // Now carries damage directly
        persistent = false,
        targetType,
        creatorId
      ),
      Hitbox()
    )

    if (bounces > 0) {
      import data.SpawnStrategy
      entity.addComponent(
        game.entity.DeathEvents(
          Seq(
            data.DeathEvents.DeathEventReference.SpawnProjectile(
              ProjectileReference.LightningBolt(
                damage, // Maintain damage
                bounces - 1,
                bounceRange,
                Some(targetType)
              ),
              Set(
                SpawnStrategy.TargetNearestEnemy(bounceRange),
                SpawnStrategy.ExcludeKiller,
                SpawnStrategy.ExcludeCreator
                // Also exclude the new projectile's creator (which is the victim of this death event)
                // But ExcludeCreator handles the victim's creator.
                // We also want to exclude the victim itself?
                // TargetNearestEnemy filters out "victim.id" in generic logic anyway?
                // Left check DeathHandlerSystem logic: "e.id != victim.id" is explicit.
              )
            )
          )
        )
      )
    } else {
      entity
    }
  }
}
