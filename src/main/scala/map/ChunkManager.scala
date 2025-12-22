package map

import game.Point
import map._

object ChunkManager {
  private val ChunkLoadingRadius = 5

  /** Updates the world map to ensure chunks around the player are loaded. Also
    * triggers structure generation for visited regions.
    */
  def updateChunks(
      playerPosition: Point,
      worldMap: WorldMap,
      config: WorldConfig,
      seed: Long
  ): WorldMap = {
    // Determine which chunks need to be loaded
    val centerChunkCoords = Chunk.toChunkCoords(playerPosition)

    val requiredChunks = (for {
      dx <- -ChunkLoadingRadius to ChunkLoadingRadius
      dy <- -ChunkLoadingRadius to ChunkLoadingRadius
    } yield (centerChunkCoords._1 + dx, centerChunkCoords._2 + dy)).toSet

    // Determine relevant regions for these chunks
    // Use a Set to avoid duplicate processing
    val relevantRegions = requiredChunks.map { case (cx, cy) =>
      val worldX = cx * Chunk.size
      val worldY = cy * Chunk.size
      val rX =
        Math.floor(worldX.toDouble / GlobalFeaturePlanner.RegionSizeTiles).toInt
      val rY =
        Math.floor(worldY.toDouble / GlobalFeaturePlanner.RegionSizeTiles).toInt
      (rX, rY)
    }

    // Generate structures for NEW relevant regions
    var worldMapWithStructures = worldMap

    // Filter out regions we've already processed
    val newRegions =
      relevantRegions.filterNot(worldMap.processedRegions.contains)

    if (newRegions.nonEmpty) {

      newRegions.foreach { case (rX, rY) =>
        worldMapWithStructures = generateStructuresForRegion(
          rX,
          rY,
          worldMapWithStructures,
          config,
          seed
        )
      }

      // Update the processed regions set
      worldMapWithStructures = worldMapWithStructures.copy(
        processedRegions = worldMapWithStructures.processedRegions ++ newRegions
      )
    }

    val missingChunks =
      requiredChunks.filterNot(worldMapWithStructures.chunks.contains)

    if (missingChunks.isEmpty) {
      worldMapWithStructures
    } else {
      // Generate new chunks
      val newChunksData = missingChunks.map { chunkCoords =>
        val chunk = generateChunk(chunkCoords, config, seed)
        (chunkCoords, chunk)
      }

      val newChunks = newChunksData.map(d => d._1 -> d._2).toMap
      val newTiles = newChunksData.flatMap(_._2.tiles).toMap

      val existingTiles = worldMapWithStructures.tiles

      // Don't overwrite existing tiles (structures)
      val filteredNewTiles =
        newTiles.filterNot(p => existingTiles.contains(p._1))

      worldMapWithStructures.copy(
        chunks = worldMapWithStructures.chunks ++ newChunks,
        tiles = existingTiles ++ filteredNewTiles
      )
    }
  }

  def generateStructuresForRegion(
      regionX: Int,
      regionY: Int,
      worldMap: WorldMap,
      config: WorldConfig,
      seed: Long
  ): WorldMap = {
    // Get the deterministic plan for this region
    val plan = GlobalFeaturePlanner.planRegion(regionX, regionY, seed)

    var updatedMap = worldMap

    // 1. Dungeons
    plan.dungeonConfig.foreach { dungeonConfig =>
      // Generates dungeon if not exists...
      val dungeonExists =
        updatedMap.dungeons.exists(_.seed == dungeonConfig.seed)
      if (!dungeonExists) {

        try {
          val dungeon = DungeonGenerator.generateDungeon(dungeonConfig)
          updatedMap = updatedMap.copy(
            dungeons = updatedMap.dungeons :+ dungeon,
            tiles = updatedMap.tiles ++ dungeon.tiles
          )
        } catch {
          case e: Exception =>
            println(s"Failed to generate dungeon: ${e.getMessage}")
        }
      }
    }

    // 2. Villages
    plan.villagePlan.foreach { villagePlan =>
      val villageExists = updatedMap.villages.exists(
        _.centerLocation == villagePlan.centerLocation
      )
      if (!villageExists) {

        try {
          val village = Village.generateVillage(
            villagePlan.centerLocation,
            villagePlan.seed
          )
          updatedMap = updatedMap.copy(
            villages = updatedMap.villages :+ village,
            tiles = updatedMap.tiles ++ village.tiles
          )
        } catch {
          case e: Exception =>
            println(s"Failed to generate village: ${e.getMessage}")
        }
      } else {}
    }

    updatedMap
  }

  /** Generates a single chunk using shared terrain logic and overlaying global
    * paths.
    */
  private def generateChunk(
      coords: (Int, Int),
      config: WorldConfig,
      seed: Long
  ): Chunk = {
    val bounds = Chunk.chunkBounds(coords)

    // 1. Generate Base Terrain
    var tiles = SharedTerrainGenerator.generateTiles(bounds, seed)

    // 2. Overlay Global Paths
    // Get plan for this chunk's region
    // Chunk coords (cx, cy) -> Region coords (floor(cx/10), floor(cy/10))
    val regionX = Math
      .floor(coords._1.toDouble / GlobalFeaturePlanner.RegionSizeChunks)
      .toInt
    val regionY = Math
      .floor(coords._2.toDouble / GlobalFeaturePlanner.RegionSizeChunks)
      .toInt

    val plan = GlobalFeaturePlanner.planRegion(regionX, regionY, seed)

    // Filter paths relevant to this chunk
    val relevantPaths = plan.globalPaths.filter { p =>
      p.x >= bounds.minRoomX && p.x <= bounds.maxRoomX &&
      p.y >= bounds.minRoomY && p.y <= bounds.maxRoomY
    }

    // Overlay path tiles
    relevantPaths.foreach { p =>
      // If it's on water, make bridge, else dirt
      val currentTile = tiles.getOrElse(p, TileType.Grass1)
      val pathTile =
        if (currentTile == TileType.Water) TileType.Bridge else TileType.Dirt
      tiles = tiles + (p -> pathTile)
    }

    Chunk(coords, tiles)
  }

  // Removed local generateChunkTerrain as we use SharedTerrainGenerator now
}
