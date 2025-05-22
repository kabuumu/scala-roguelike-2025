package game.entity

import data.Sprites
import game.Constants.DEFAULT_EXP
import game.entity.EntityType.Projectile
import game.entity.Health.*
import game.entity.Hitbox.*
import game.entity.Experience.*
import game.entity.UpdateAction.{CollisionCheckAction, WaveUpdateAction}
import game.{Constants, GameState, Point}

import java.util.UUID

case class Collision(damage: Int, explodes: Boolean, persistent: Boolean, target: EntityType, creatorId: String) extends Component {
  private def handleCollision(parentEntity: Entity, collidingEntity: Entity, gameState: GameState): GameState = {
    if (collidingEntity[EntityTypeComponent].entityType == target && collidingEntity.isAlive) {
      if (explodes) {
        val explosionEntity = Entity(
          s"explosion ${UUID.randomUUID()}",
          Hitbox(Set(Point(0, 0))),
          parentEntity[Movement],
          Collision(damage = damage, explodes = false, persistent = true, target, creatorId),
          UpdateController(CollisionCheckAction, WaveUpdateAction),
          Drawable(Sprites.projectileSprite),
          Wave(2),
          EntityTypeComponent(Projectile)
        )

        gameState
          .add(explosionEntity)
          .remove(parentEntity)
      } else {
        val damagedEntity = collidingEntity.damage(damage)

        gameState.updateEntity(damagedEntity.id, damagedEntity) match {
          case gameState if damagedEntity.isDead =>
            gameState
              .updateEntity(creatorId, _.addExperience(DEFAULT_EXP))
              .addMessage(s"$creatorId killed ${damagedEntity.id} and gained $DEFAULT_EXP experience")
          case gameState =>
            gameState
        } match {
          case gameState if persistent =>
            gameState
          case gameState =>
            gameState
              .remove(parentEntity)
        }
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

