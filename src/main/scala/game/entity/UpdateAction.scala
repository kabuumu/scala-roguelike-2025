package game.entity

import game.entity.Collision.*
import game.entity.Health.*
import game.entity.Initiative.*
import game.entity.Projectile.*
import game.entity.Wave.*
import game.event.{DecreaseInitiativeEvent, Event, MarkForDeathEvent}
import game.{DeathDetails, EnemyAI, GameState, Point}

trait UpdateAction {
  def apply(entity: Entity, gameState: GameState): Seq[Event]
}

object UpdateAction {
  case object UpdateInitiative extends UpdateAction {
    override def apply(entity: Entity, gameState: GameState): Seq[Event] =
      if (entity.isAlive && entity.notReady)
        Seq(DecreaseInitiativeEvent(entity.id))
      else
        Nil
  }

  case class AIAction(ai: EnemyAI) extends UpdateAction {
    override def apply(entity: Entity, gameState: GameState): Seq[Event] = {
      if (entity.isReady)
        ai.getNextAction(entity, gameState).apply(entity, gameState)
      else
        Nil
    }
  }

  object ProjectileUpdateAction extends UpdateAction {
    override def apply(entity: Entity, gameState: GameState): Seq[Event] = {
      entity.projectileUpdate()
    }
  }

  object CollisionCheckAction extends UpdateAction {
    override def apply(entity: Entity, gameState: GameState): Seq[Event] = {
      entity.collisionCheck(gameState)
    }
  }

  object WaveUpdateAction extends UpdateAction {
    override def apply(entity: Entity, gameState: GameState): Seq[Event] = {
      entity.waveUpdate(gameState)
    }
  }

  object RangeCheckAction extends UpdateAction {
    override def apply(entity: Entity, gameState: GameState): Seq[Event] = {
      entity.get[Projectile] match {
        case Some(projectile) if projectile.isAtTarget =>
          Seq(MarkForDeathEvent(entity.id, DeathDetails(entity)))
        case _ =>
          Nil
      }
    }
  }
}

