package game.system

import game.GameState
import game.entity.*
import game.entity.EntityType
import game.entity.EntityType.entityType
import game.system.event.GameSystemEvent
import map.TileType
import game.Point
import data.Enemies

object WildAnimalSpawnSystem extends GameSystem {

  private val CheckInterval = 20
  private val SpawnRadius = 40
  private val MinAnimalsInRadius = 5
  private val SpawnBatchSize = 3 // Try to spawn this many if needed

  // Ensure we don't spawn right next to player (keep it somewhat out of sight if possible, or at least not on top)
  private val MinSpawnDistance = 15

  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent.GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    // Only run in Adventure mode where wild animals exist
    if (gameState.gameMode != game.GameMode.Adventure) {
      return (gameState, Seq.empty)
    }

    val player = gameState.playerEntity

    // Check if it's time to run logic
    player.get[WildAnimalSpawnTracker] match {
      case Some(tracker) =>
        if (tracker.ticksSinceLastSpawn >= CheckInterval) {
          // Time to check
          val playerPos =
            player.get[Movement].map(_.position).getOrElse(Point(0, 0))

          // Count animals in radius
          val nearbyAnimals = gameState.entities.count { e =>
            e.entityType == EntityType.Animal &&
            e.get[Movement]
              .exists(_.position.isWithinRangeOf(playerPos, SpawnRadius))
          }

          if (nearbyAnimals < MinAnimalsInRadius) {
            // Need to spawn more
            val (newWorldMap, newEntities) = spawnAnimals(gameState, playerPos)

            val updatedPlayer = player.update[WildAnimalSpawnTracker](_.reset)
            // Add new entities and update player tracker
            val newGameState = gameState
              .updateEntity(player.id, updatedPlayer)
              .copy(
                entities = gameState.entities // We'll append new ones
                // Actually simplest is to just append
              )

            // Add entities to state
            // Note: We might have updated worldMap if we needed to (we don't for entity spawn usually, unless we track metadata)
            // But here we rely on standard spawning.
            // Better to use `gameState.add(entity)` pattern

            val finalState = newEntities.foldLeft(newGameState) {
              (state, entity) =>
                state.add(entity)
            }

            (finalState, Seq.empty)
          } else {
            // No spawn needed, just reset tracker (or keep ticking? Usually reset to wait another interval)
            // Actually if we just checked and didn't spawn, we should still reset to avoid checking every single tick after this.
            val updatedPlayer = player.update[WildAnimalSpawnTracker](_.reset)
            (gameState.updateEntity(player.id, updatedPlayer), Seq.empty)
          }
        } else {
          // Increment tracker
          val updatedPlayer = player.update[WildAnimalSpawnTracker](_.tick)
          (gameState.updateEntity(player.id, updatedPlayer), Seq.empty)
        }

      case None =>
        // Player doesn't have tracker, shouldn't happen if initialized correctly.
        (gameState, Seq.empty)
    }
  }

  private def spawnAnimals(
      gameState: GameState,
      center: Point
  ): (map.WorldMap, Seq[Entity]) = {
    val worldMap = gameState.worldMap
    val random = new scala.util.Random()
    val newEntities = scala.collection.mutable.ListBuffer[Entity]()

    // Try to spawn a few animals
    for (_ <- 1 to SpawnBatchSize) {
      // Find valid spot
      // Random angle and distance
      val angle = random.nextDouble() * 2 * Math.PI
      val distance =
        MinSpawnDistance + random.nextInt(SpawnRadius - MinSpawnDistance)

      val x = (center.x + distance * Math.cos(angle)).toInt
      val y = (center.y + distance * Math.sin(angle)).toInt
      val p = Point(x, y)

      // Check validity
      val isValid = worldMap.tiles.get(p) match {
        case Some(TileType.Grass1) | Some(TileType.Grass2) |
            Some(TileType.Grass3) | Some(TileType.Dirt) =>
          !worldMap.rocks.contains(p) && !worldMap.water.contains(
            p
          ) && !worldMap.walls.contains(p) &&
          !gameState
            .getVisiblePointsFor(gameState.playerEntity)
            .contains(p) // Ensure out of sight!
        case _ => false
      }

      if (isValid) {
        newEntities += Enemies.duck(
          s"spawned-duck-${System.currentTimeMillis()}-${random.nextInt()}",
          p
        )
      }
    }

    (worldMap, newEntities.toSeq)
  }
}
