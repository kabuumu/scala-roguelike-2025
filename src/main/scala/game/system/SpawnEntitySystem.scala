package game.system

import data.Sprites
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.entity.Movement.position
import game.GameState
import game.entity.SightMemory.*
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*

object SpawnEntitySystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val updatedGamestate = events.foldLeft(gameState) {
      case (currentState, SpawnEntityEvent(entity)) =>
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
