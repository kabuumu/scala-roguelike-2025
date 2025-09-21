package game.system

import data.Entities.EntityReference.Explosion
import data.Projectiles.ProjectileReference.*
import data.{Entities, Projectiles, Sprites}
import game.GameState
import game.entity.*
import game.entity.EntityType.{Enemy, entityType}
import game.entity.Experience.experienceForLevel
import game.entity.Movement.position
import game.entity.SightMemory.*
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*

object SpawnProjectileSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedGamestate = events.foldLeft(gameState) {
      case (currentState, SpawnProjectileEvent(projectileReference, creator, targetPoint)) =>
        val projectile = projectileReference match {
          case Arrow =>
            Projectiles.arrowProjectile(
              creator.id,
              creator.position,
              targetPoint,
              if (gameState.getEntity(creator.id).exists(_.entityType == EntityType.Player)) EntityType.Enemy else EntityType.Player)
          case Fireball =>
            Projectiles.fireballProjectile(
              creator.id,
              creator.position,
              targetPoint,
              if (gameState.getEntity(creator.id).exists(_.entityType == EntityType.Player)) EntityType.Enemy else EntityType.Player)
          case SnakeSpit =>
            Projectiles.snakeSpitProjectile(
              creator.id,
              creator.position,
              targetPoint,
              if (gameState.getEntity(creator.id).exists(_.entityType == EntityType.Player)) EntityType.Enemy else EntityType.Player)
          case BossBlast =>
            Projectiles.bossBlastProjectile(
              creator.id,
              creator.position,
              targetPoint,
              if (gameState.getEntity(creator.id).exists(_.entityType == EntityType.Player)) EntityType.Enemy else EntityType.Player)
        }
        
        currentState.add(projectile)
      case (currentState, _) =>
        currentState
    }

    (updatedGamestate, Nil)
  }
}
