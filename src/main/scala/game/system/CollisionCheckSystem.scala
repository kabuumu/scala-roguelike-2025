package game.system

import game.GameState
import game.entity.Hitbox.*
import game.system.event.GameSystemEvent.{CollisionEvent, CollisionTarget, GameSystemEvent}

object CollisionCheckSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val collisionEvents = for {
      //TODO - consolidate collision systems in the future
//      entity <- gameState.entities
      otherEntity <- gameState.entities
      entity = gameState.playerEntity // For now, only check collisions for the player entity
      if entity.id != otherEntity.id && entity.collidesWith(otherEntity)
    } yield CollisionEvent(
      entityId = entity.id,
      collidedWith = CollisionTarget.Entity(otherEntity.id)
    )
    
//    val wallCollisionEvents = for {
//      entity <- gameState.entities
//      if entity.collidesWith(gameState.dungeon.walls)
//    } yield CollisionEvent(
//      entityId = entity.id,
//      collidedWith = CollisionTarget.Wall
//    )

    (gameState, collisionEvents)
  }
}
