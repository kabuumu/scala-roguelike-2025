package game.entity

import data.Sprites
import game.entity.EntityType.Projectile
import game.entity.Health.*
import game.entity.Hitbox.*
import game.entity.UpdateAction.{CollisionCheckAction, WaveUpdateAction}
import game.{GameState, Point}

import java.util.UUID

case class Collision(damage: Int, explodes: Boolean, persistent: Boolean, target: EntityType) extends Component {
  private def handleCollision(parentEntity: Entity, collidingEntity: Entity, gameState: GameState): GameState = {
    if (collidingEntity[EntityTypeComponent].entityType == target && collidingEntity.isAlive) {
      if (explodes) {
        val explosionEntity = Entity(
          s"explosion ${UUID.randomUUID()}",
          Hitbox(Set(Point(0, 0))),
          parentEntity[Movement],
          Collision(damage = damage, explodes = false, persistent = true, target),
          UpdateController(CollisionCheckAction, WaveUpdateAction),
          Drawable(Sprites.projectileSprite),
          Wave(2),
          EntityTypeComponent(Projectile)
        )

        gameState
          .add(explosionEntity)
          .remove(parentEntity)
      } else if (persistent) {
        gameState
          .updateEntity(collidingEntity.id, _.damage(damage))
      } else {
        gameState
          .updateEntity(collidingEntity.id, _.damage(damage))
          .remove(parentEntity)
      }
    } else {
      gameState
    }
  }
}

object Collision {
  extension (entity: Entity) {
    private def handleCollision(gameState: GameState, collidingEntity: Entity): GameState = entity.get[Collision] match {
      case Some(collisionComponent) => collisionComponent.handleCollision(entity, collidingEntity, gameState)
      case None => gameState
    }

    def collisionCheck(gameState: GameState): GameState = {
      val collisionHitbox = entity.hitbox

      if (gameState.dungeon.walls.intersect(collisionHitbox).nonEmpty)
            gameState.remove(entity)
      else gameState.entities.filter(entity.collidesWith).foldLeft(gameState){
        case (state, collidingEntity) =>
          handleCollision(state, collidingEntity)
      }
    }
  }
}

