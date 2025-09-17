package game.system

import data.Entities.EntityReference.{Explosion, Slimelet}
import data.{Entities, Sprites}
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.entity.Movement.position
import game.{GameState, Point}
import game.entity.EntityType.Enemy
import game.entity.SightMemory.*
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*

object SpawnEntitySystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedGamestate = events.foldLeft(gameState) {
      case (currentState, SpawnEntityEvent(entityReference, creator, wantedSpawnPosition, forceSpawn)) =>
        val spawnPosition = if (forceSpawn) {
          // Forced spawn - use the wanted position directly
          wantedSpawnPosition
        } else {
          // Non-forced spawn - find nearest non-colliding position
          val directions = Seq(
            (0, 0), // Original position
            (1, 0), (-1, 0), (0, 1), (0, -1), // Cardinal directions
            (1, 1), (1, -1), (-1, 1), (-1, -1) // Diagonal directions
          ).map { case (dx, dy) => Point(dx, dy) }

          directions
            .map(offset => wantedSpawnPosition + offset)
            .find(pos => !currentState.movementBlockingPoints.contains(pos))
            .getOrElse(wantedSpawnPosition) // Fallback to original position if all are blocked
        }
        
        val entity: Entity = entityReference match {
          case Explosion(damage, size) =>
            Entities.explosionEffect(creator.id, spawnPosition, Enemy, damage, size) //TODO - Defaulted to targetType Enemy, should be parameterized
          case Slimelet =>
            Entities.slimelet(spawnPosition)
        }
        // Regular spawn - no collision checking
        currentState.add(entity)
      case (currentState, SpawnEntityWithCollisionCheckEvent(entityTemplate, preferredPositions)) =>
        // Collision-checked spawn - find first empty position
        val emptyPositions = preferredPositions.filterNot(currentState.movementBlockingPoints.contains)
        if (emptyPositions.nonEmpty) {
          val spawnPosition = emptyPositions.head
          val entity = entityTemplate.update[Movement](_.copy(position = spawnPosition))
          currentState.add(entity)
        } else {
          // No empty positions available, don't spawn
          currentState
        }
      case (currentState, _) =>
        currentState
    }

    (updatedGamestate, Nil)
  }
}
