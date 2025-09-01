package game.event

import game.GameState
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Movement.position
import game.system.event.GameSystemEvent.SpawnEntityEvent
import data.Sprites

import scala.util.Random

/**
 * Event for creating projectiles with optional explosion on death.
 * Replaces ItemEffect.CreateProjectile to unify the effect system.
 * 
 * @param creatorId The entity creating the projectile
 * @param targetPoint Where the projectile should go
 * @param targetEntity Optional target entity (takes precedence over targetPoint)
 * @param collisionDamage Damage dealt on collision
 * @param onDeathExplosion Optional explosion effect when projectile dies
 */
case class CreateProjectileEvent(
  creatorId: String,
  targetPoint: game.Point,
  targetEntity: Option[Entity] = None,
  collisionDamage: Int,
  onDeathExplosion: Option[ExplosionEffect] = None
) extends Event {
  
  override def apply(gameState: GameState): (GameState, Seq[Event]) = {
    gameState.getEntity(creatorId) match {
      case Some(creator) =>
        val targetType = if (creator.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
        val startingPosition = creator.position
        
        // Determine the final target position for the projectile
        val finalTargetPoint = targetEntity.map(_.position).getOrElse(targetPoint)

        // Create the projectile entity
        val projectileEntity = Entity(
          id = s"Projectile-${System.nanoTime()}",
          Movement(position = startingPosition),
          game.entity.Projectile(startingPosition, finalTargetPoint, targetType, collisionDamage),
          EntityTypeComponent(EntityType.Projectile),
          Drawable(Sprites.projectileSprite),
          Collision(damage = collisionDamage, persistent = false, targetType, creatorId),
          Hitbox()
        )

        // Add explosion behavior if specified - reusing the OnDeath pattern from the original system
        val finalProjectileEntity = onDeathExplosion match {
          case Some(explosion) =>
            projectileEntity.addComponent(DeathEvents(
              deathDetails => Seq(SpawnEntityEvent(createExplosionEntity(deathDetails.victim, explosion, targetType, creatorId)))
            ))
          case None =>
            projectileEntity
        }

        val addEntityEvent = AddEntityEvent(finalProjectileEntity)
        (gameState.add(finalProjectileEntity), Nil)
        
      case None =>
        // Creator not found, cannot create projectile
        (gameState, Nil)
    }
  }
  
  /** 
   * Creates an explosion entity on projectile death - reuses the pattern from the original ComponentItemUseSystem
   */
  private def createExplosionEntity(parentEntity: Entity, explosion: ExplosionEffect, targetType: EntityType, creatorId: String): Entity = {
    Entity(
      s"explosion ${Random.nextInt()}",
      Hitbox(Set(game.Point(0, 0))),
      Collision(damage = explosion.damage, persistent = true, targetType, creatorId),
      Movement(position = parentEntity.position),
      Drawable(Sprites.projectileSprite),
      Wave(explosion.radius),
      EntityTypeComponent(EntityType.Projectile) // Consistent with original system
    )
  }
}