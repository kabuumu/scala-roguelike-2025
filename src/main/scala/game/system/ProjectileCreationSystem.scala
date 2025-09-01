package game.system

import game.GameState
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Movement.position
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{CreateProjectileEvent, SpawnEntityEvent}
import data.Sprites

import scala.util.Random

/**
 * System that handles projectile creation events from the GameSystemEvent architecture.
 * Replaces the deprecated CreateProjectileEvent from the old Event system.
 */
object ProjectileCreationSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val (updatedGameState, newEvents) = events.foldLeft((gameState, Seq.empty[GameSystemEvent.GameSystemEvent])) {
      case ((currentState, currentEvents), GameSystemEvent.CreateProjectileEvent(creatorId, targetPoint, targetEntityId, collisionDamage, onDeathExplosion)) =>
        currentState.getEntity(creatorId) match {
          case Some(creator) =>
            val targetType = if (creator.entityType == EntityType.Player) EntityType.Enemy else EntityType.Player
            val startingPosition = creator.position
            
            // Determine the final target position for the projectile
            val finalTargetPoint = targetEntityId.flatMap(currentState.getEntity).map(_.position).getOrElse(targetPoint)

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
                  deathDetails => Seq(GameSystemEvent.SpawnEntityEvent(createExplosionEntity(deathDetails.victim, explosion, targetType, creatorId)))
                ))
              case None =>
                projectileEntity
            }

            val spawnEvent = GameSystemEvent.SpawnEntityEvent(finalProjectileEntity)
            (currentState, currentEvents :+ spawnEvent)
            
          case None =>
            // Creator not found, cannot create projectile
            (currentState, currentEvents)
        }
      case ((currentState, currentEvents), _) =>
        // Not a projectile creation event, pass through
        (currentState, currentEvents)
    }
    
    (updatedGameState, newEvents)
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