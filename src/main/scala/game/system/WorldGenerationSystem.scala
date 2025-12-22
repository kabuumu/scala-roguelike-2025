package game.system

import game.GameState
import game.system.event.GameSystemEvent.GameSystemEvent
import map.{ChunkManager, WorldConfig, MapBounds}
import game.entity.Movement.position

object WorldGenerationSystem extends GameSystem {

  // Default config matching typically used values
  // Ideally this should be stored in WorldMap or GameState
  private val defaultWorldConfig = WorldConfig(
    bounds = MapBounds(
      -1000,
      1000,
      -1000,
      1000
    ), // Large bounds for "infinite" generation
    grassDensity = 0.6,
    treeDensity = 0.15,
    dirtDensity = 0.25,
    perimeterTrees = false
  )

  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {
    // Only update world generation if we are in Adventure mode
    if (gameState.gameMode != game.GameMode.Adventure) {
      return (gameState, Seq.empty)
    }

    val playerPos = gameState.playerEntity.position
    val worldMap = gameState.worldMap

    // Seed should be from the map itself
    val seed = worldMap.seed

    // Update chunks
    val newWorldMap = ChunkManager.updateChunks(
      playerPos,
      worldMap,
      defaultWorldConfig,
      seed
    )

    // If map changed, return new state
    if (newWorldMap != worldMap) {
      (gameState.copy(worldMap = newWorldMap), Seq.empty)
    } else {
      (gameState, Seq.empty)
    }
  }
}
