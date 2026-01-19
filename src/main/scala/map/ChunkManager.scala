package map

import game.Point
import map._

object ChunkManager {
  private val ChunkLoadingRadius = 5

  /** Updates the world map to ensure chunks around the player are loaded. Also
    * triggers structure generation for visited regions.
    */
  /** Updates the world map to ensure chunks around the player are loaded. Also
    * unloads chunks that are too far away. Triggers structure generation for
    * visited regions.
    */
  def updateChunks(
      playerPosition: Point,
      worldMap: WorldMap,
      config: WorldConfig,
      seed: Long
  ): WorldMap = {
    // Determine which chunks need to be loaded
    val centerChunkCoords = Chunk.toChunkCoords(playerPosition)

    // Optimization: If we are still in the same chunk as last update, DO NOTHING.
    // This avoids all set allocations and map filtering 99% of frames.
    if (worldMap.lastCenterChunk.contains(centerChunkCoords)) {
      return worldMap
    }

    val requiredChunks = (for {
      dx <- -ChunkLoadingRadius to ChunkLoadingRadius
      dy <- -ChunkLoadingRadius to ChunkLoadingRadius
    } yield (centerChunkCoords._1 + dx, centerChunkCoords._2 + dy)).toSet

    // 1. Identify chunks to UNLOAD (active chunks that are not effectively required)
    // We add a small buffer preventing rapid load/unload at boundary
    val unloadRadius = ChunkLoadingRadius + 2
    val chunksToKeep = worldMap.chunks.filter { case (coords, _) =>
      val dx = Math.abs(coords._1 - centerChunkCoords._1)
      val dy = Math.abs(coords._2 - centerChunkCoords._2)
      dx <= unloadRadius && dy <= unloadRadius
    }

    // 2. Identify NEW chunks
    val missingChunks = requiredChunks.filterNot(worldMap.chunks.contains)

    // 3. Identify NEW REGIONS
    val relevantRegions = requiredChunks.map { case (cx, cy) =>
      val worldX = cx * Chunk.size
      val worldY = cy * Chunk.size
      val rX =
        Math.floor(worldX.toDouble / GlobalFeaturePlanner.RegionSizeTiles).toInt
      val rY =
        Math.floor(worldY.toDouble / GlobalFeaturePlanner.RegionSizeTiles).toInt
      (rX, rY)
    }
    val newRegions =
      relevantRegions.filterNot(worldMap.processedRegions.contains)

    // Optimization: If no changes needed, return map but UPDATE lastCenterChunk
    if (
      chunksToKeep.size == worldMap.chunks.size && missingChunks.isEmpty && newRegions.isEmpty
    ) {
      return worldMap.copy(lastCenterChunk = Some(centerChunkCoords))
    }

    // Calculate new active chunks by removing unloaded ones
    val activeChunksMap = chunksToKeep

    // 4. Generate structures for NEW relevant regions
    var worldMapWithStructures = worldMap.copy(chunks = activeChunksMap)

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

    val newChunksMap: Map[(Int, Int), Chunk] = if (missingChunks.isEmpty) {
      Map.empty
    } else {
      missingChunks.map { chunkCoords =>
        val chunk = generateChunk(chunkCoords, config, seed)
        chunkCoords -> chunk
      }.toMap
    }

    // 5. Re-assemble final map

    // Refined Flow:
    // 1. `activeChunks` (Kept chunks).
    // 2. `freshChunks` (New generated chunks).
    // 3. `freshChunks` -> merge ALL existing global structure tiles (to restore structure parts in reloaded chunks).
    // 4. `activeChunks` -> merge NEW structure tiles (if a new dungeon spawned nearby).

    // Helper to merge tiles into chunks
    def mergeTilesIntoChunks(
        tiles: Map[Point, TileType],
        chunks: Map[(Int, Int), Chunk]
    ): Map[(Int, Int), Chunk] = {
      tiles.foldLeft(chunks) { case (acc, (point, tile)) =>
        val chunkCoords = Chunk.toChunkCoords(point)
        if (acc.contains(chunkCoords)) {
          val chunk = acc(chunkCoords)
          acc.updated(chunkCoords, chunk.mergeTiles(Map(point -> tile)))
        } else {
          acc // Ignore tiles for chunks not currently managed (shouldn't happen for local structures)
        }
      }
    }

    val freshChunksWithOverlay =
      freshChunksOverlay(newChunksMap, worldMapWithStructures)

    var finalChunksMap = activeChunksMap ++ freshChunksWithOverlay

    // Now apply NEW structures to ALL (overlap check included)
    // Actually `worldMapWithStructures` has the updated lists.
    // If we just apply `newDungeons` and `newVillages` to `finalChunksMap`, we catch everything.
    // New chunks already possess "Old" structures from `freshChunksOverlay`.
    // Active chunks might need "New" structures.

    // Let's identify the NEW structures added since `worldMap`.
    val newDungeons = worldMapWithStructures.dungeons.diff(worldMap.dungeons)
    val newVillages = worldMapWithStructures.villages.diff(worldMap.villages)

    val newStructureTiles =
      newDungeons.flatMap(_.tiles).toMap ++ newVillages.flatMap(_.tiles).toMap

    if (newStructureTiles.nonEmpty) {
      finalChunksMap = mergeTilesIntoChunks(newStructureTiles, finalChunksMap)
    }

    // Final Tile Map assembly
    val allTiles = finalChunksMap.values.flatMap(_.tiles).toMap

    worldMapWithStructures.copy(
      chunks = finalChunksMap,
      tiles = allTiles,
      lastCenterChunk = Some(centerChunkCoords)
    )
  }

  private def freshChunksOverlay(
      newChunks: Map[(Int, Int), Chunk],
      worldMap: WorldMap
  ): Map[(Int, Int), Chunk] = {
    if (newChunks.isEmpty) {
      return Map.empty
    }

    val structureTiles = worldMap.dungeons
      .flatMap(_.tiles)
      .toMap ++ worldMap.villages.flatMap(_.tiles).toMap

    newChunks.map { case (coords, chunk) =>
      val changes = structureTiles.filter { case (p, _) =>
        Chunk.toChunkCoords(p) == coords
      }
      if (changes.nonEmpty) coords -> chunk.mergeTiles(changes)
      else coords -> chunk
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
