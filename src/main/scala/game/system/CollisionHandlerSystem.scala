package game.system

import game.entity.{
  Collision,
  EntityType,
  EntityTypeComponent,
  Health,
  MarkedForDeath
}
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{CollisionEvent, CollisionTarget}
import game.{DeathDetails, GameState}
import Health.*

import game.entity.Movement.position

object CollisionHandlerSystem extends GameSystem {
  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent.GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val collisionEvents = events.collect { case event: CollisionEvent =>
      event
    }

    collisionEvents.foldLeft((gameState, Nil)) {
      case (
            (currentGameState, currentEvents),
            CollisionEvent(entityId, collisionTarget)
          ) =>
        (currentGameState.getEntity(entityId), collisionTarget) match {
          case (Some(entity), CollisionTarget.Wall)
              if entity.get[Collision].exists(_.persistent == false) =>
            (
              currentGameState.updateEntity(
                entityId,
                _.addComponent(
                  MarkedForDeath(DeathDetails(entity, Some(entityId)))
                )
              ),
              currentEvents
            )
          case (Some(entity), CollisionTarget.Entity(collidedEntityId)) =>
            (
              entity.get[Collision],
              currentGameState.getEntity(collidedEntityId)
            ) match {
              case (Some(collisionComponent), Some(collidingEntity))
                  if (collidingEntity
                    .get[EntityTypeComponent]
                    .exists(_.entityType == collisionComponent.target) ||
                    (collisionComponent.target == EntityType.Enemy && collidingEntity
                      .get[EntityTypeComponent]
                      .exists(_.entityType == EntityType.Animal))) &&
                    collidingEntity.isAlive &&
                    collidingEntity.id != collisionComponent.creatorId =>
                // if collision is hitting its target entity type, cause damage
                // if colliding entity is not persistent, then mark it for death
                var events = currentEvents :+ GameSystemEvent.DamageEvent(
                  collidingEntity.id,
                  collisionComponent.creatorId,
                  collisionComponent.damage,
                  GameSystemEvent.DamageSource.Projectile
                )

                val updatedGameState =
                  if (collisionComponent.persistent) currentGameState
                  else
                    currentGameState.updateEntity(
                      entity.id,
                      _.addComponent(
                        MarkedForDeath(
                          DeathDetails(entity, Some(collidedEntityId))
                        )
                      )
                    )

                (updatedGameState, events)
              case _ => (currentGameState, currentEvents)
            }
          case _ => (currentGameState, currentEvents)
        }
    }
  }
}
