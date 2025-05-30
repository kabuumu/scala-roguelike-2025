package game.entity

import game.entity.EntityType.*
import game.entity.Health.*
import game.entity.Hitbox.*
import game.event.*
import game.{DeathDetails, GameState}
import game.entity.Movement.*

import scala.language.postfixOps

case class Collision(damage: Int, persistent: Boolean, target: EntityType, creatorId: String) extends Component {
  private def handleCollision(parentEntity: Entity, collidingEntity: Entity, gameState: GameState): Seq[Event] = {
    if (collidingEntity.entityType == target && collidingEntity.isAlive) {
        Seq(
          DamageEntityEvent(collidingEntity.id, damage, creatorId)
        ) ++ (if (persistent) {
          Nil
        } else {
          Seq(MarkForDeathEvent(parentEntity.id, DeathDetails(parentEntity)))
        })
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
      val persistent = entity.get[Collision].exists(_.persistent)

      if (gameState.dungeon.walls.intersect(collisionHitbox).nonEmpty && !persistent)
          Seq(MarkForDeathEvent(entity.id, DeathDetails(entity)))
      else gameState.entities.filter(entity.collidesWith).flatMap { 
        collidingEntity =>
          handleCollision(gameState, collidingEntity)
      }
    }
  }
}

