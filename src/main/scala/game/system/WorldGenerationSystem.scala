package game.system

import game.GameState
import game.system.event.GameSystemEvent.GameSystemEvent
import map.{ChunkManager, WorldConfig, MapBounds}
import game.entity.Movement.position
import data.Crops
import data.Farmer
import data.TraderData
import map.BuildingType

import map.TileType

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
      // Check for new villages and spawn crops
      val oldVillages = worldMap.villages.toSet
      val newVillages = newWorldMap.villages.toSet
      val brandNewVillages = newVillages.diff(oldVillages)

      val cropEntities = brandNewVillages.flatMap { village =>
        village.buildings
          .filter(_.buildingType == BuildingType.Farmland)
          .flatMap { farm =>
            farm.tiles.collect { case (point, TileType.Farmland) =>
              // Spawn Wheat!
              // Add some randomness to initial growth so they don't all pop at once
              val crop = Crops.wheat(
                s"wheat-${seed}-${point.x}-${point.y}",
                point
              )

              // Randomize initial growth stage
              val randomStage = scala.util.Random.nextInt(4) // 0 to 3 inclusive

              crop
                .get[game.entity.Growth]
                .map { g =>
                  if (randomStage > 0) {
                    val stageSprite =
                      g.stageSprites.getOrElse(randomStage, g.stageSprites(0))
                    crop
                      .update[game.entity.Growth](_ =>
                        g.copy(
                          currentStage = randomStage,
                          growthTimer = g.timePerStage
                        )
                      )
                      .update[game.entity.Drawable](_ =>
                        game.entity.Drawable(stageSprite)
                      )
                  } else {
                    // Just randomize timer for stage 0
                    val randomTimer = g.growthTimer + (seed % 300).toInt
                    crop.update[game.entity.Growth](_ =>
                      g.copy(growthTimer = randomTimer)
                    )
                  }
                }
                .getOrElse(crop)
            }
          }
      }

      // Spawn Farmers (ensure every village has one)
      // Check all villages in the new map. If a village doesn't have a farmer near it, spawn one.
      val villagesNeedingFarmers = newWorldMap.villages.filter { v =>
        // simplistic check: is there any entity with Harvester component within 30 tiles of center?
        !gameState.entities.exists(e =>
          e.has[game.entity.Harvester] && e.position.getChebyshevDistance(
            v.centerLocation
          ) < 30
        )
      }

      val farmers = villagesNeedingFarmers.map { village =>
        // Find a valid spawn point. Village center might be inside a wall.
        // Use a path tile or entrance which is guaranteed to be walkable.
        val spawnPos = village.paths.headOption
          .orElse(village.entrances.headOption)
          .getOrElse(village.centerLocation)

        Farmer.create(
          s"farmer-${seed}-${spawnPos.x}-${spawnPos.y}-${System.currentTimeMillis()}",
          spawnPos
        )
      }

      val allNewEntities = cropEntities ++ farmers

      // Spawn occasional caravans (limited number)
      // Check if we need a caravan and have a village to spawn at
      val existingCaravanCount =
        gameState.entities.count(_.has[game.entity.CaravanComponent])
      val caravans =
        if (existingCaravanCount < 1 && newWorldMap.villages.nonEmpty) {
          // Spawn at the first village available (either new or existing)
          // Prefer new villages if available, otherwise just pick one
          val village =
            brandNewVillages.headOption.getOrElse(newWorldMap.villages.head)

          // Spawn near village center
          TraderData.createCaravan(
            s"caravan-${seed}-${System.currentTimeMillis()}",
            village.centerLocation
          )
        } else {
          Seq.empty
        }

      val allEntitiesWithCaravans = allNewEntities ++ caravans

      val finalState =
        allEntitiesWithCaravans.foldLeft(
          gameState.copy(worldMap = newWorldMap)
        ) { (state, entity) =>
          state.add(entity)
        }

      (finalState, Seq.empty)
    } else {
      (gameState, Seq.empty)
    }
  }
}
