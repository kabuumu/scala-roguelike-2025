package map

import game.{Direction, Point}

case class VillagePlan(
    centerLocation: Point,
    seed: Long
)

case class RegionPlan(
    dungeonConfig: Option[DungeonConfig],
    villagePlan: Option[VillagePlan],
    globalPaths: Set[Point]
)

object GlobalFeaturePlanner {
  val RegionSizeChunks = 5
  val RegionSizeTiles = RegionSizeChunks * 16 // 80 tiles
  // 80 is divisible by 16 (Chunk) and 10 (Dungeon Room). Perfect alignment.

  // Cache to store calculated region plans
  private val cache =
    scala.collection.mutable.Map[(Int, Int, Long), RegionPlan]()

  /*
   * Generates a plan for a specific region.
   * This is a deterministic function of region coordinates and world seed.
   */
  def planRegion(regionX: Int, regionY: Int, seed: Long): RegionPlan = {
    cache.getOrElseUpdate(
      (regionX, regionY, seed), {
        val regionSeed = seed + (regionX * 10000) + regionY
        val random = new scala.util.Random(regionSeed)

        val regionMinX = regionX * RegionSizeTiles
        val regionMinY = regionY * RegionSizeTiles

        // 2. Determine Connection Points (Roads at edges)
        // ----------------------------------------------
        // Since we are guaranteed to be clear of edges, any consistent edge point is fine.

        val leftRoadY =
          getConsistentEdgeOffset(regionX, regionY, isVerticalEdge = true, seed)
        val leftPoint = Point(regionMinX, regionMinY + leftRoadY)

        val rightRoadY =
          getConsistentEdgeOffset(
            regionX + 1,
            regionY,
            isVerticalEdge = true,
            seed
          )
        val rightPoint =
          Point(regionMinX + RegionSizeTiles - 1, regionMinY + rightRoadY)

        val topRoadX =
          getConsistentEdgeOffset(
            regionX,
            regionY,
            isVerticalEdge = false,
            seed
          )
        val topPoint = Point(regionMinX + topRoadX, regionMinY)

        val bottomRoadX =
          getConsistentEdgeOffset(
            regionX,
            regionY + 1,
            isVerticalEdge = false,
            seed
          )
        val bottomPoint =
          Point(regionMinX + bottomRoadX, regionMinY + RegionSizeTiles - 1)

        val hubX = (topRoadX + bottomRoadX) / 2 + regionMinX
        val hubY = (leftRoadY + rightRoadY) / 2 + regionMinY
        val hub = Point(hubX, hubY)

        // 1. Determine local features (Dungeon, Village)
        // ---------------------------------------------
        // We place features strictly in the center area to avoid edge collisions.
        // Region is 80x80.
        // Padding of 15 tiles on each side leaves 50x50 area for features.
        // This guarantees 30 tiles (15+15) between features in adjacent regions.

        val hasFeature =
          random.nextDouble() < 0.90 || (regionX == 0 && regionY == 0)

        var dungeonConfig: Option[DungeonConfig] = None
        var villagePlan: Option[VillagePlan] = None
        var obstacles: Set[Point] = Set.empty

        if (hasFeature) {
          val isVillage =
            if (regionX == 0 && regionY == 0) true else random.nextBoolean()

          // Strict bounds for feature placement
          val padding = 20
          val validMinX = regionMinX + padding
          val validMaxX = regionMinX + RegionSizeTiles - padding
          val validMinY = regionMinY + padding
          val validMaxY = regionMinY + RegionSizeTiles - padding
          val validWidth = validMaxX - validMinX
          val validHeight = validMaxY - validMinY

          if (isVillage) {
            // Village Logic
            // Place somewhere in valid area
            val villageX = validMinX + random.nextInt(validWidth)
            val villageY = validMinY + random.nextInt(validHeight)
            val location = Point(villageX, villageY)

            villagePlan = Some(VillagePlan(location, regionSeed))

            // Add village as obstacle for local paths
            val obsRadius = 15
            for (ox <- -obsRadius to obsRadius; oy <- -obsRadius to obsRadius) {
              obstacles += Point(location.x + ox, location.y + oy)
            }
          } else {
            // Dungeon Logic
            // Generate bounds strictly within valid area

            // Available area in ROOM coords (10 tiles)
            // validMinX is e.g. 15. In room coords, let's round up to be safe?
            // Or just work in room coords from the start.

            val roomSize = Dungeon.roomSize // 10

            // Calculate valid room indices
            // Region 0: 0 to 80.
            // Padding 15. Valid: 15 to 65.
            // Room coords: 1.5 to 6.5.
            // We need integer room coords.
            // Min room index: ceil(15/10) = 2.
            // Max room index: floor(65/10) = 6.
            // So rooms can be at 2, 3, 4, 5. (Width 4 rooms max if completely filling).
            // Actually, a room at x=5 ends at 59. Room at x=6 ends at 69. 69 > 65? No.
            // Room at x starts at x*10. Ends at x*10+9.
            // if max valid pixel is 65. 6*10+9 = 69. So 6 is unsafe if strict padding.
            // Let's use simple logic:

            // Safe room range relative to region start:
            // Region is 0-8 rooms (80 tiles).
            // Padding 1.5 rooms. Safe zone: rooms [2, 5]. (Indices 2,3,4,5).
            // 4 rooms max available width.

            val availableRooms =
              (RegionSizeTiles - 2 * padding) / roomSize // (80-30)/10 = 5 rooms.
            val maxRoomDim =
              math.min(4, availableRooms) // Cap at 4x4 rooms for Dungeon size

            var roomWidth = random.nextInt(maxRoomDim - 1) + 2 // 2 to max
            var roomHeight = random.nextInt(maxRoomDim - 1) + 2

            // Ensure area >= 5 for valid dungeon generation
            while (roomWidth * roomHeight < 5) {
              roomWidth = random.nextInt(maxRoomDim - 1) + 2
              roomHeight = random.nextInt(maxRoomDim - 1) + 2
            }

            // Available range for top-left corner
            // bounds in room coords relative to region
            val startOffsetRooms = (padding + roomSize - 1) / roomSize // 2
            // max start offset = total available spaces - width
            // e.g. 5 available slots, width 2. can start at 0, 1, 2, 3. (3+2=5).
            val maxStartIndexX = availableRooms - roomWidth
            val maxStartIndexY = availableRooms - roomHeight

            val offX = random.nextInt(maxStartIndexX + 1)
            val offY = random.nextInt(maxStartIndexY + 1)

            val absMinRoomX = (regionMinX / roomSize) + startOffsetRooms + offX
            val absMinRoomY = (regionMinY / roomSize) + startOffsetRooms + offY

            val bounds = MapBounds(
              absMinRoomX,
              absMinRoomX + roomWidth - 1,
              absMinRoomY,
              absMinRoomY + roomHeight - 1
            )

            val availableArea = roomWidth * roomHeight
            val targetSize =
              math.max(5, math.min(12, (availableArea * 0.5).toInt))

            val entranceSide =
              if (hubX < absMinRoomX * roomSize) Direction.Left
              else if (hubX > (absMinRoomX + roomWidth) * roomSize)
                Direction.Right
              else if (hubY < absMinRoomY * roomSize) Direction.Up
              else Direction.Down

            dungeonConfig = Some(
              DungeonConfig(
                bounds = bounds,
                seed = regionSeed,
                explicitSize = Some(targetSize),
                entranceSide = entranceSide
              )
            )

            // Add dungeon bounds as obstacles
            val (dMinX, dMaxX, dMinY, dMaxY) =
              bounds.toTileBounds(Dungeon.roomSize)
            for (x <- dMinX to dMaxX; y <- dMinY to dMaxY) {
              obstacles += Point(x, y)
            }
          }
        }

        // Adjust hub if it falls on an obstacle
        val adjustedHub = if (obstacles.contains(hub)) {
          // BFS to find nearest non-obstacle
          val queue = scala.collection.mutable.Queue(hub)
          val visited = scala.collection.mutable.Set(hub)
          var found: Option[Point] = None

          while (queue.nonEmpty && found.isEmpty) {
            val current = queue.dequeue()
            if (!obstacles.contains(current)) {
              found = Some(current)
            } else {
              val neighbors = Seq(
                Point(current.x + 1, current.y),
                Point(current.x - 1, current.y),
                Point(current.x, current.y + 1),
                Point(current.x, current.y - 1)
              )

              neighbors.foreach { n =>
                if (
                  !visited.contains(n) &&
                  n.x >= regionMinX && n.x < regionMinX + RegionSizeTiles &&
                  n.y >= regionMinY && n.y < regionMinY + RegionSizeTiles
                ) {
                  visited += n
                  queue.enqueue(n)
                }
              }
            }
          }
          found.getOrElse(hub)
        } else {
          hub
        }

        // 3. Connect Points (Paths)
        // -------------------------

        // Helper to generate safe path
        def safePath(p1: Point, p2: Point): Set[Point] = {
          // We use the entire region bounds for pathfinding limit, plus some buffer?
          // Actually global pathfinding needs to be constrained or it might wander too far.
          // Let's constrain to this region + some overlap.
          val pathBounds = MapBounds(
            (regionMinX / 10),
            (regionMinX + RegionSizeTiles) / 10,
            (regionMinY / 10),
            (regionMinY + RegionSizeTiles) / 10
          )

          PathGenerator.generatePathAroundObstacles(
            p1,
            p2,
            obstacles,
            width = 1,
            pathBounds
          )
        }

        val mainPaths =
          safePath(leftPoint, adjustedHub) ++
            safePath(rightPoint, adjustedHub) ++
            safePath(topPoint, adjustedHub) ++
            safePath(bottomPoint, adjustedHub)

        // Connect Features
        val featurePath = dungeonConfig match {
          case Some(d) =>
            // Path from entrance point to hub
            val startRoom = d.getEntranceRoom
            val entranceSide = d.entranceSide
            val entrancePoint =
              Dungeon.getApproachTile(startRoom, entranceSide)

            PathGenerator.generatePathAroundObstacles(
              entrancePoint,
              adjustedHub,
              obstacles - entrancePoint,
              width = 1,
              MapBounds(
                (regionMinX / 10),
                (regionMinX + RegionSizeTiles) / 10,
                (regionMinY / 10),
                (regionMinY + RegionSizeTiles) / 10
              )
            )
          case None =>
            villagePlan match {
              case Some(v) =>
                // Remove village area from obstacles to allow path to exit
                val r = 15
                val villageArea = (for {
                  ox <- -r to r
                  oy <- -r to r
                } yield Point(
                  v.centerLocation.x + ox,
                  v.centerLocation.y + oy
                )).toSet

                PathGenerator.generatePathAroundObstacles(
                  v.centerLocation,
                  adjustedHub,
                  obstacles -- villageArea,
                  width = 1,
                  MapBounds(
                    (regionMinX / 10),
                    (regionMinX + RegionSizeTiles) / 10,
                    (regionMinY / 10),
                    (regionMinY + RegionSizeTiles) / 10
                  )
                )
              case None => Set.empty[Point]
            }
        }

        RegionPlan(
          dungeonConfig,
          villagePlan,
          mainPaths ++ featurePath
        )
      }
    )
  }

  /*
   * Returns a consistent offset (0 to RegionSizeTiles-1) for a road crossing a boundary.
   * If isVerticalEdge is true, we are looking for Y offset on the vertical line at X.
   * If false, we look for X offset on the horizontal line at Y.
   * 
   * The semantic of (x,y) here is the INDEX of the boundary line.
   */
  private def getConsistentEdgeOffset(
      idx1: Int,
      idx2: Int,
      isVerticalEdge: Boolean,
      seed: Long
  ): Int = {
    // Hash inputs to get a stable random number
    val h1 = (idx1 * 374761393L) ^ (idx2 * 668265263L)
    val h2 = h1 ^ (if (isVerticalEdge) 123456789L else 987654321L)
    val h3 = h2 ^ seed

    // Mix bits
    val mixed = (h3 ^ (h3 >>> 16)) * 0x85ebca6bL
    val finalHash = (mixed ^ (mixed >>> 13)) * 0xc2b2ae35L

    // Map to valid range [10, RegionSizeTiles-10] to avoid corners
    val range = RegionSizeTiles - 20
    Math.abs(finalHash % range).toInt + 10
  }

}
