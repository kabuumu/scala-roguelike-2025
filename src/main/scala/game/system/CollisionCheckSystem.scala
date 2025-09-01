package game.system

import game.GameState
import game.entity.Hitbox.*
import game.entity.*
import game.entity.EntityType.entityType
import game.entity.Movement.position
import game.system.event.GameSystemEvent.{CollisionEvent, CollisionTarget, GameSystemEvent}

object CollisionCheckSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    // Only check collisions for entities that are not marked for death
    val collidableEntities = gameState.entities.filter(_.has[Hitbox]).filter(!_.has[MarkedForDeath])
    
    val collisionEvents = for {
      //TODO - consolidate collision systems in the future
      entity <- collidableEntities
      otherEntity <- collidableEntities
      if entity.id != otherEntity.id && entity.collidesWith(otherEntity)
    } yield CollisionEvent(
      entityId = entity.id,
      collidedWith = CollisionTarget.Entity(otherEntity.id)
    )
    
    val wallCollisionEvents = for {
      entity <- collidableEntities
      if entity.collidesWith(gameState.dungeon.walls)
    } yield CollisionEvent(
      entityId = entity.id,
      collidedWith = CollisionTarget.Wall
    )

    (gameState, collisionEvents ++ wallCollisionEvents)
  }
}
