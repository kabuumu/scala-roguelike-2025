package map

import game.Point
import map._

object ChunkManager {
  private val ChunkLoadingRadius = 5

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
    if (worldMap.lastCenterChunk.contains(centerChunkCoords)) {
      return worldMap
    }

    val requiredChunks = (for {
      dx <- -ChunkLoadingRadius to ChunkLoadingRadius
      dy <- -ChunkLoadingRadius to ChunkLoadingRadius
    } yield (centerChunkCoords._1 + dx, centerChunkCoords._2 + dy)).toSet

    // 1. Identify chunks to UNLOAD
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
      // Region calculation must match GlobalFeaturePlanner
      // cx is Chunk Coordinate. Region is RegionSizeChunks wide.
      val regionX =
        Math.floor(cx.toDouble / GlobalFeaturePlanner.RegionSizeChunks).toInt
      val regionY =
        Math.floor(cy.toDouble / GlobalFeaturePlanner.RegionSizeChunks).toInt
      (regionX, regionY)
    }
    val newRegions =
      relevantRegions.filterNot(worldMap.processedRegions.contains)

    if (
      chunksToKeep.size == worldMap.chunks.size && missingChunks.isEmpty && newRegions.isEmpty
    ) {
      return worldMap.copy(lastCenterChunk = Some(centerChunkCoords))
    }

    val activeChunksMap = chunksToKeep
    var worldMapWithStructures = worldMap.copy(chunks = activeChunksMap)

    // 4. Generate structures for NEW relevant regions
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

      worldMapWithStructures = worldMapWithStructures.copy(
        processedRegions = worldMapWithStructures.processedRegions ++ newRegions
      )
    }

    // 5. Generate NEW chunks
    val newChunksMap: Map[(Int, Int), Chunk] = missingChunks.map {
      chunkCoords =>
        val chunk =
          generateChunk(chunkCoords, config, seed, worldMapWithStructures)
        chunkCoords -> chunk
    }.toMap

    // 6. Merge Tiles
    val freshChunksWithOverlay =
      freshChunksOverlay(newChunksMap, worldMapWithStructures)
    var finalChunksMap = activeChunksMap ++ freshChunksWithOverlay

    val newDungeons = worldMapWithStructures.dungeons.diff(worldMap.dungeons)
    val newVillages = worldMapWithStructures.villages.diff(worldMap.villages)

    val newStructureTiles =
      newDungeons.flatMap(_.tiles).toMap ++ newVillages.flatMap(_.tiles).toMap

    if (newStructureTiles.nonEmpty) {
      finalChunksMap = mergeTilesIntoChunks(newStructureTiles, finalChunksMap)
    }

    val allTiles = finalChunksMap.values.flatMap(_.tiles).toMap

    worldMapWithStructures.copy(
      chunks = finalChunksMap,
      tiles = allTiles,
      lastCenterChunk = Some(centerChunkCoords)
    )
  }

  private def mergeTilesIntoChunks(
      tiles: Map[Point, TileType],
      chunks: Map[(Int, Int), Chunk]
  ): Map[(Int, Int), Chunk] = {
    tiles.foldLeft(chunks) { case (acc, (point, tile)) =>
      val chunkCoords = Chunk.toChunkCoords(point)
      if (acc.contains(chunkCoords)) {
        val chunk = acc(chunkCoords)
        acc.updated(chunkCoords, chunk.mergeTiles(Map(point -> tile)))
      } else {
        acc
      }
    }
  }

  private def freshChunksOverlay(
      newChunks: Map[(Int, Int), Chunk],
      worldMap: WorldMap
  ): Map[(Int, Int), Chunk] = {
    if (newChunks.isEmpty) {
      return Map.empty
    }

    val structureTiles = worldMap.dungeons.flatMap(_.tiles).toMap ++
      worldMap.villages.flatMap(_.tiles).toMap

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
    var updatedMap = worldMap

    if (worldMap.overworldMap.isDefined) {
      // OVERWORLD MODE
      val om = worldMap.overworldMap.get

      // Calculate Overworld Bounds for this region
      val startCx = regionX * GlobalFeaturePlanner.RegionSizeChunks
      val startCy = regionY * GlobalFeaturePlanner.RegionSizeChunks
      val endCx = startCx + GlobalFeaturePlanner.RegionSizeChunks - 1
      val endCy = startCy + GlobalFeaturePlanner.RegionSizeChunks - 1

      // Find villages in this region
      // Check each village location to see if it's within the region bounds (in overworld coords)
      val localVillages = om.villages.filter { v =>
        v.x >= startCx && v.x <= endCx && v.y >= startCy && v.y <= endCy
      }

      localVillages.foreach { v =>
        // Determine detailed center
        // Village is a 2x2 Overworld cluster (40x40 tiles).
        // To center it perfectly within the 2x2 area, we place center at the corner of the top-left tile.
        // v.x is the top-left tile.
        // Center = (v.x * 20) + 20.
        val detailedX = v.x * Chunk.size + Chunk.size
        val detailedY = v.y * Chunk.size + Chunk.size
        val center = Point(detailedX, detailedY)

        // Only generate if not already present (avoid duplicates)
        if (!updatedMap.villages.exists(_.centerLocation == center)) {
          try {
            // Generate logic similar to Village.generateVillage but we must ensure seed is unique
            val village = Village.generateVillage(
              center,
              seed ^ (v.x * 73856093L) ^ (v.y * 19349663L)
            )
            updatedMap = updatedMap.copy(
              villages = updatedMap.villages :+ village,
              tiles = updatedMap.tiles ++ village.tiles
            )
          } catch {
            case e: Exception =>
              println(s"Failed to generate village: ${e.getMessage}")
          }
        }
      }

      // Generate structures for towns (3x3 overworld cluster)
      val localTowns = om.towns.filter { t =>
        t.x >= startCx && t.x <= endCx && t.y >= startCy && t.y <= endCy
      }

      localTowns.foreach { t =>
        // Town is a 3x3 Overworld cluster (60x60 tiles).
        // Center at the middle of the cluster.
        val detailedX = t.x * Chunk.size + (Chunk.size * 3) / 2
        val detailedY = t.y * Chunk.size + (Chunk.size * 3) / 2
        val center = Point(detailedX, detailedY)

        if (!updatedMap.villages.exists(_.centerLocation == center)) {
          try {
            val town = Village.generateTown(
              center,
              seed ^ (t.x * 73856093L) ^ (t.y * 19349663L) ^ 70005L
            )
            updatedMap = updatedMap.copy(
              villages = updatedMap.villages :+ town,
              tiles = updatedMap.tiles ++ town.tiles
            )
          } catch {
            case e: Exception =>
              println(s"Failed to generate town: ${e.getMessage}")
          }
        }
      }

      // Generate structures for cities (5x5 overworld cluster)
      val localCities = om.cities.filter { c =>
        c.x >= startCx && c.x <= endCx && c.y >= startCy && c.y <= endCy
      }

      localCities.foreach { c =>
        // City is a 5x5 Overworld cluster (100x100 tiles).
        // Center at the middle of the cluster.
        val detailedX = c.x * Chunk.size + (Chunk.size * 5) / 2
        val detailedY = c.y * Chunk.size + (Chunk.size * 5) / 2
        val center = Point(detailedX, detailedY)

        if (!updatedMap.villages.exists(_.centerLocation == center)) {
          try {
            val city = Village.generateCity(
              center,
              seed ^ (c.x * 73856093L) ^ (c.y * 19349663L) ^ 31747L
            )
            updatedMap = updatedMap.copy(
              villages = updatedMap.villages :+ city,
              tiles = updatedMap.tiles ++ city.tiles
            )
          } catch {
            case e: Exception =>
              println(s"Failed to generate city: ${e.getMessage}")
          }
        }
      }

    } else {
      // CLASSIC MODE
      val plan = GlobalFeaturePlanner.planRegion(regionX, regionY, seed)

      // 1. Dungeons
      plan.dungeonConfig.foreach { dungeonConfig =>
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
        }
      }
    }

    updatedMap
  }

  private def generateChunk(
      coords: (Int, Int),
      config: WorldConfig,
      seed: Long,
      worldMap: WorldMap
  ): Chunk = {
    val bounds = Chunk.chunkBounds(coords)

    // Determine Base Tiles
    var tiles: Map[Point, TileType] = worldMap.overworldMap match {
      case Some(om) =>
        // Overworld Mode: Use Overworld Tile
        // Chunk Coords (cx, cy) == Overworld Tile Coords
        val ox = coords._1
        val oy = coords._2

        om.getTile(Point(ox, oy)) match {
          case Some(overworldTile) =>
            generateTilesForOverworldType(bounds, seed, overworldTile, ox, oy, om)
          case None =>
            // Out of bounds - Ocean/Water/Void?
            // Use fallback terrain
            SharedTerrainGenerator.generateTiles(bounds, seed)
        }

      case None =>
        // Classic Mode
        SharedTerrainGenerator.generateTiles(bounds, seed)
    }

    // Classic Mode Global Paths Overlay (Only if NO OverworldMap)
    if (worldMap.overworldMap.isEmpty) {
      val regionX = Math
        .floor(coords._1.toDouble / GlobalFeaturePlanner.RegionSizeChunks)
        .toInt
      val regionY = Math
        .floor(coords._2.toDouble / GlobalFeaturePlanner.RegionSizeChunks)
        .toInt
      val plan = GlobalFeaturePlanner.planRegion(regionX, regionY, seed)

      val relevantPaths = plan.globalPaths.filter { p =>
        p.x >= bounds.minRoomX && p.x <= bounds.maxRoomX &&
        p.y >= bounds.minRoomY && p.y <= bounds.maxRoomY
      }

      relevantPaths.foreach { p =>
        val currentTile = tiles.getOrElse(p, TileType.Grass1)
        val pathTile =
          if (currentTile == TileType.Water) TileType.Bridge else TileType.Dirt
        tiles = tiles + (p -> pathTile)
      }
    }

    Chunk(coords, tiles)
  }

  /** Check if an overworld tile type is connectable for road rendering. */
  private def isConnectable(t: OverworldTileType): Boolean = t match {
    case OverworldTileType.Road | OverworldTileType.Bridge |
        OverworldTileType.Path | OverworldTileType.PathBridge |
        OverworldTileType.Trail | OverworldTileType.TrailBridge |
        OverworldTileType.City | OverworldTileType.Town |
        OverworldTileType.Village | OverworldTileType.Beach =>
      true
    case _ => false
  }

  /** Check if an overworld tile type is a road/path/trail. */
  private def isRoadLike(t: OverworldTileType): Boolean = t match {
    case OverworldTileType.Road | OverworldTileType.Bridge |
        OverworldTileType.Path | OverworldTileType.PathBridge |
        OverworldTileType.Trail | OverworldTileType.TrailBridge =>
      true
    case _ => false
  }

  /** Get the half-width for a road-like tile type. */
  private def roadHalfWidth(t: OverworldTileType): Double = t match {
    case OverworldTileType.Trail | OverworldTileType.TrailBridge => 1.0
    case OverworldTileType.Path | OverworldTileType.PathBridge   => 2.0
    case _                                                       => 4.0
  }

  /** Check if an overworld tile type is a settlement. */
  private def isSettlement(t: OverworldTileType): Boolean = t match {
    case OverworldTileType.City | OverworldTileType.Town |
        OverworldTileType.Village =>
      true
    case _ => false
  }

  private def generateTilesForOverworldType(
      bounds: MapBounds,
      seed: Long,
      tileType: OverworldTileType,
      ox: Int = 0,
      oy: Int = 0,
      overworldMap: OverworldMap = OverworldMap(Map.empty, 0L, 0, 0)
  ): Map[Point, TileType] = {
    // Determine which neighbors are connectable for directional road rendering
    val connectLeft = overworldMap.getTile(Point(ox - 1, oy)).exists(isConnectable)
    val connectRight = overworldMap.getTile(Point(ox + 1, oy)).exists(isConnectable)
    val connectUp = overworldMap.getTile(Point(ox, oy - 1)).exists(isConnectable)
    val connectDown = overworldMap.getTile(Point(ox, oy + 1)).exists(isConnectable)

    val chunkSize = bounds.maxRoomX - bounds.minRoomX + 1
    val center = (chunkSize - 1) / 2.0

    (for {
      x <- bounds.minRoomX to bounds.maxRoomX
      y <- bounds.minRoomY to bounds.maxRoomY
    } yield {
      val point = Point(x, y)
      val standardTile = SharedTerrainGenerator.generateTile(x, y, seed)

      val finalTile = tileType match {
        case OverworldTileType.Ocean | OverworldTileType.Water =>
          TileType.Water

        case OverworldTileType.Beach =>
          TileType.Dirt

        case OverworldTileType.Mountain =>
          if (standardTile == TileType.Wall) TileType.Wall else TileType.Rock

        case OverworldTileType.Forest =>
          val baseTile =
            if (standardTile == TileType.Water) TileType.Grass1
            else standardTile

          val rnd = new scala.util.Random(seed ^ (x * 39119L) ^ (y * 65327L))
          baseTile match {
            case TileType.Grass1 | TileType.Grass2 | TileType.Grass3 =>
              if (rnd.nextDouble() < 0.5) TileType.Tree else baseTile
            case TileType.Dirt =>
              if (rnd.nextDouble() < 0.5) TileType.Tree else TileType.Grass1
            case _ =>
              TileType.Tree
          }

        case OverworldTileType.Village | OverworldTileType.Town |
            OverworldTileType.City =>
          // Draw internal road connections through the settlement area
          val lx = x - bounds.minRoomX
          val ly = y - bounds.minRoomY
          val dx = Math.abs(lx - center)
          val dy = Math.abs(ly - center)

          // Check each neighbor for road-like or settlement tiles
          val leftNeighbor = overworldMap.getTile(Point(ox - 1, oy))
          val rightNeighbor = overworldMap.getTile(Point(ox + 1, oy))
          val upNeighbor = overworldMap.getTile(Point(ox, oy - 1))
          val downNeighbor = overworldMap.getTile(Point(ox, oy + 1))

          // Connect to road-like neighbors with matching road width
          val leftHW = leftNeighbor.filter(isRoadLike).map(roadHalfWidth).getOrElse(0.0)
          val rightHW = rightNeighbor.filter(isRoadLike).map(roadHalfWidth).getOrElse(0.0)
          val upHW = upNeighbor.filter(isRoadLike).map(roadHalfWidth).getOrElse(0.0)
          val downHW = downNeighbor.filter(isRoadLike).map(roadHalfWidth).getOrElse(0.0)

          // Also connect to neighboring settlement tiles (internal paths, width 2.0)
          val settlementHW = 2.0
          val connectLeftSettlement = leftNeighbor.exists(isSettlement)
          val connectRightSettlement = rightNeighbor.exists(isSettlement)
          val connectUpSettlement = upNeighbor.exists(isSettlement)
          val connectDownSettlement = downNeighbor.exists(isSettlement)

          val leftW = math.max(leftHW, if (connectLeftSettlement) settlementHW else 0.0)
          val rightW = math.max(rightHW, if (connectRightSettlement) settlementHW else 0.0)
          val upW = math.max(upHW, if (connectUpSettlement) settlementHW else 0.0)
          val downW = math.max(downHW, if (connectDownSettlement) settlementHW else 0.0)

          val onRoadConnection = {
            val leftOfCenter = lx < center
            val rightOfCenter = lx > center
            val aboveCenter = ly < center
            val belowCenter = ly > center

            (dy < leftW && leftOfCenter && leftW > 0) ||
            (dy < rightW && rightOfCenter && rightW > 0) ||
            (dx < upW && aboveCenter && upW > 0) ||
            (dx < downW && belowCenter && downW > 0)
          }

          if (onRoadConnection) TileType.Dirt else TileType.Grass1

        case OverworldTileType.Road | OverworldTileType.Bridge |
            OverworldTileType.Path | OverworldTileType.PathBridge |
            OverworldTileType.Trail | OverworldTileType.TrailBridge =>

          val halfWidth = tileType match {
            case OverworldTileType.Trail | OverworldTileType.TrailBridge => 1.0
            case OverworldTileType.Path | OverworldTileType.PathBridge   => 2.0
            case _                                                       => 4.0
          }

          val lx = x - bounds.minRoomX
          val ly = y - bounds.minRoomY

          val dx = Math.abs(lx - center)
          val dy = Math.abs(ly - center)

          // Only draw road lines toward connected neighbors
          val onHorizontalStrip = dy < halfWidth // Within the horizontal band
          val onVerticalStrip = dx < halfWidth   // Within the vertical band

          // Check if this pixel is in a direction that has a connection
          val inConnectedArea = {
            val leftOfCenter = lx < center
            val rightOfCenter = lx > center
            val aboveCenter = ly < center
            val belowCenter = ly > center

            // Center hub: always draw if at least one connection exists
            val atCenter = onHorizontalStrip && onVerticalStrip
            val anyConnection = connectLeft || connectRight || connectUp || connectDown

            (atCenter && anyConnection) ||
            (onHorizontalStrip && leftOfCenter && connectLeft) ||
            (onHorizontalStrip && rightOfCenter && connectRight) ||
            (onVerticalStrip && aboveCenter && connectUp) ||
            (onVerticalStrip && belowCenter && connectDown)
          }

          if (inConnectedArea) {
            if (
              tileType == OverworldTileType.Bridge ||
              tileType == OverworldTileType.PathBridge ||
              tileType == OverworldTileType.TrailBridge
            ) TileType.Bridge
            else TileType.Dirt
          } else {
            standardTile
          }

        case OverworldTileType.Plains =>
          if (standardTile == TileType.Water) TileType.Grass1 else standardTile

        case OverworldTileType.Desert =>
          TileType.Dirt
      }

      point -> finalTile
    }).toMap
  }
}
