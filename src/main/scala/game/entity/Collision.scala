package game.entity

import data.Sprites
import game.Constants.DEFAULT_EXP
import game.entity.EntityType.Projectile
import game.entity.Experience.*
import game.entity.Health.*
import game.entity.Hitbox.*
import game.entity.UpdateAction.{CollisionCheckAction, WaveUpdateAction}
import game.event.*
import game.{Constants, GameState, Point}

import java.util.UUID
import scala.language.postfixOps

case class Collision(damage: Int, explodes: Boolean, persistent: Boolean, target: EntityType, creatorId: String) extends Component {
  private def handleCollision(parentEntity: Entity, collidingEntity: Entity, gameState: GameState): Seq[Event] = {
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

        Seq(
          AddEntityEvent(explosionEntity),
          RemoveEntityEvent(parentEntity.id)
        )
      } else {
        val damagedEntity = collidingEntity.damage(damage)

        Seq(
          DamageEntityEvent(collidingEntity.id, damage)
        ) ++ (if(damagedEntity.isDead) {
          Seq(
            RemoveEntityEvent(collidingEntity.id),
            AddExperienceEvent(creatorId, DEFAULT_EXP)
          )
        } else {
          Nil
        }) ++ (if(persistent) {
          Nil
        } else {
          Seq(RemoveEntityEvent(parentEntity.id))
        })
      }
    } else {
      Nil
    }
  }
}

object Collision {
  extension (entity: Entity) {
    private def handleCollision(gameState: GameState, collidingEntity: Entity): Seq[Event] = entity.get[Collision] match {
      case Some(collisionComponent) => collisionComponent.handleCollision(entity, collidingEntity, gameState)
      case None => Nil
    }

    def collisionCheck(gameState: GameState): Seq[Event] = {
      val collisionHitbox = entity.hitbox

      if (gameState.dungeon.walls.intersect(collisionHitbox).nonEmpty)
          Seq(RemoveEntityEvent(entity.id))
      else gameState.entities.filter(entity.collidesWith).flatMap { 
        collidingEntity =>
          handleCollision(gameState, collidingEntity)
      }
    }
  }
}

