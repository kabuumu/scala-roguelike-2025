package game.system

import game.entity.{Collision, EntityTypeComponent, Health, MarkedForDeath}
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{CollisionEvent, CollisionTarget}
import game.{DeathDetails, GameState}
import Health.*

object CollisionHandlerSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) =
    events.collect {
      case event: CollisionEvent => event
    }.foldLeft((gameState, Nil)) {
      case ((currentGameState, currentEvents), CollisionEvent(entityId, collisionTarget)) =>
        (gameState.getEntity(entityId), collisionTarget) match {
          case (Some(entity), CollisionTarget.Wall) if entity.get[Collision].exists(_.persistent == false) =>
            (currentGameState.updateEntity(entityId, _.addComponent(MarkedForDeath(DeathDetails(entity)))), currentEvents)
          case (Some(entity), CollisionTarget.Entity(collidedEntityId)) =>
            (entity.get[Collision], currentGameState.getEntity(collidedEntityId)) match {
              case (Some(collisionComponent), Some(collidingEntity)) if collidingEntity.get[EntityTypeComponent].exists(_.entityType == collisionComponent.target) && collidingEntity.isAlive =>
                //if collision is hitting its target entity type, cause damage
                //if colliding entity is not persistent, then mark it for death
                val events = currentEvents :+ GameSystemEvent.DamageEvent(collidingEntity.id, entity.id, collisionComponent.damage)
                val updatedGameState = if (collisionComponent.persistent) currentGameState
                else currentGameState.updateEntity(entity.id, _.addComponent(MarkedForDeath(DeathDetails(entity))))

                (updatedGameState, events)
              case _ => (currentGameState, currentEvents)
            }
          case _ => (currentGameState, currentEvents)
        }
    }
}