package game

import data.DeathEvents.DeathEventReference.{GiveExperience, SpawnEntity}
import data.Entities.EntityReference.Slimelet
import data.{Enemies, Items, Sprites}
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.entity.Movement.position
import game.system.event.GameSystemEvent.{AddExperienceEvent, SpawnEntityWithCollisionCheckEvent}
import map.{Dungeon, MapGenerator, WorldMapGenerator, WorldMapConfig, WorldConfig, MapBounds, RiverConfig, DungeonConfig, PathGenerator, TileType}


object StartingState {
  // Generate an open world with grass, dirt, trees, rivers, and dungeons
  // World size: 21x21 room grid to allow bounded dungeon generation
  // Provides enough space for dungeons with all required features
  val worldSize = 10  // Room radius (full grid is 2*worldSize+1 = 21x21 rooms)
  
  private val worldBounds = MapBounds(-worldSize, worldSize, -worldSize, worldSize)  // Moderate world size
  
  println(s"[StartingState] Generating world map with bounds: $worldBounds")
  // PERFORMANCE: Make world generation lazy to avoid computation until needed
  // First generate world without paths (we'll add player-to-dungeon path after player spawn is determined)
  private lazy val initialWorldMap = WorldMapGenerator.generateWorldMap(
    WorldMapConfig(
      worldConfig = WorldConfig(
        bounds = worldBounds,
        grassDensity = 0.65,
        treeDensity = 0.20,
        dirtDensity = 0.10,
        ensureWalkablePaths = true,
        perimeterTrees = true,
        seed = System.currentTimeMillis()
      ),
      dungeonConfigs = Seq(
        // Add a single dungeon to the world
        // Use explicit configuration to ensure minimum features for gameplay
        DungeonConfig(
          bounds = worldBounds,
          seed = System.currentTimeMillis(),
          explicitSize = Some(15),  // Ensure enough rooms for good gameplay
          explicitLockedDoorCount = Some(1),  // At least one locked door for keys
          explicitItemCount = Some(3)  // At least 3 items for loot
        )
      ),
      riverConfigs = Seq(
        // Rivers flow through the center of the world so they're visible
        // World bounds in tiles: (-100, 110) x (-100, 110)
        RiverConfig(
          startPoint = Point(-60, -80),    // Start from upper left area
          flowDirection = (1, 1),           // Flow diagonally down-right
          length = 120,                     // Long river
          width = 3,                        // Visible width
          curviness = 0.3,                  // 30% chance of curves
          bounds = worldBounds,
          seed = System.currentTimeMillis()
        ),
        RiverConfig(
          startPoint = Point(80, -70),      // Start from upper right area
          flowDirection = (-1, 1),          // Flow diagonally down-left
          length = 110,                     // Long river
          width = 2,                        // Visible width
          curviness = 0.25,                 // 25% chance of curves
          bounds = worldBounds,
          seed = System.currentTimeMillis() + 1
        )
      ),
      generatePathsToDungeons = false,      // We'll add player-to-dungeon path manually
      generatePathsBetweenDungeons = false, // Only one dungeon
      pathsPerDungeon = 0,
      pathWidth = 1,
      minDungeonSpacing = 10
    )
  )
  
  println(s"[StartingState] Initial world map generated with ${initialWorldMap.tiles.size} tiles")
  println(s"[StartingState] Rivers: ${initialWorldMap.rivers.size} river tiles")

  // Create player's starting inventory items as entities
  val playerStartingItems: Set[Entity] = Set(
    Items.healingPotion("player-potion-1"),
    Items.healingPotion("player-potion-2"),
    Items.fireballScroll("player-scroll-1"),
    Items.fireballScroll("player-scroll-2"),
    Items.bow("player-bow-1")
  ) ++ (1 to 6).map(i => Items.arrow(s"player-arrow-$i"))
  
  // Create player's starting equipment
  val playerStartingEquipment: Set[Entity] = Set(
    Items.basicSword("player-starting-sword"),
    Items.chainmailArmor("player-starting-armor")
  )

  /**
   * Generate enemy groups based on dungeon depth progression.
   * Deeper rooms have harder enemies and larger groups.
   */
  object EnemyGeneration {
    import Enemies.EnemyReference
    import Enemies.EnemyDifficulty._
    
    case class EnemyGroup(enemies: Seq[EnemyReference])
    
    /**
     * Determine appropriate enemy groups for a given dungeon depth.
     * Examples: depth 1 -> 1 slimelet, depth 2 -> 2 slimelets, etc.
     */
    def enemiesForDepth(depth: Int): EnemyGroup = depth match {
      case d if d == Int.MaxValue => EnemyGroup(Seq(EnemyReference.Boss)) // Boss room - check first!
      case 1 => EnemyGroup(Seq(EnemyReference.Slimelet))
      case 2 => EnemyGroup(Seq(EnemyReference.Slimelet, EnemyReference.Slimelet))
      case 3 => EnemyGroup(Seq(EnemyReference.Slime))
      case 4 => EnemyGroup(Seq(EnemyReference.Slime, EnemyReference.Slimelet))
      case 5 => EnemyGroup(Seq(EnemyReference.Rat))
      case 6 => EnemyGroup(Seq(EnemyReference.Snake))
      case d if d >= 7 && d % 2 == 1 => EnemyGroup(Seq(EnemyReference.Rat, EnemyReference.Rat)) // Multiple rats
      case d if d >= 8 && d % 2 == 0 => EnemyGroup(Seq(EnemyReference.Snake, EnemyReference.Snake)) // Multiple snakes
      case _ => EnemyGroup(Seq(EnemyReference.Slimelet)) // Fallback for depth 0 or unexpected values
    }
    
    /**
     * Create enemy entities for a room based on its depth and position.
     */
    def createEnemiesForRoom(roomPoint: Point, depth: Int, roomIndex: Int): (Seq[Entity], Map[String, Entity]) = {
      val roomCenter = Point(
        roomPoint.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        roomPoint.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      
      val enemyGroup = enemiesForDepth(depth)
      val enemies = enemyGroup.enemies.zipWithIndex.map { case (enemyRef, enemyIndex) =>
        val enemyId = s"${enemyRef.toString}-R$roomIndex-$enemyIndex"
        val position = if (enemyIndex == 0) roomCenter else {
          // Offset additional enemies slightly to avoid overlap
          Point(roomCenter.x + enemyIndex, roomCenter.y + enemyIndex)
        }
        
        enemyRef match {
          case EnemyReference.Rat => Enemies.rat(enemyId, position)
          case EnemyReference.Snake => 
            val spitId = s"$enemyId-spit"
            Enemies.snake(enemyId, position, spitId)
          case EnemyReference.Slime => Enemies.slime(enemyId, position)
          case EnemyReference.Slimelet => Enemies.slimelet(enemyId, position)
          case EnemyReference.Boss => 
            val bossBlastId = s"$enemyId-blast"
            Enemies.boss(enemyId, position, bossBlastId)
        }
      }
      
      // Create snake spit abilities for any snakes and boss blast abilities for bosses
      val spitAbilities = enemies.collect {
        case snake if snake.id.contains("Snake") =>
          val spitId = s"${snake.id}-spit"
          spitId -> Items.snakeSpit(spitId)
        case boss if boss.id.contains("Boss") =>
          val blastId = s"${boss.id}-blast"
          blastId -> Items.bossBlast(blastId)
      }.toMap
      
      (enemies, spitAbilities)
    }
  }

  // Determine player spawn point
  private lazy val playerSpawnPoint = findPlayerSpawnPoint(initialWorldMap, worldBounds)
  
  // Generate path from player spawn to dungeon entrance
  private lazy val playerToDungeonPath: Set[Point] = initialWorldMap.primaryDungeon match {
    case Some(dungeon) =>
      // Find an accessible walkable tile in the starting room
      val startRoomX = dungeon.startPoint.x * Dungeon.roomSize
      val startRoomY = dungeon.startPoint.y * Dungeon.roomSize
      
      // Get all tiles in the starting room
      val startRoomTiles = (for {
        x <- startRoomX to (startRoomX + Dungeon.roomSize)
        y <- startRoomY to (startRoomY + Dungeon.roomSize)
      } yield Point(x, y)).toSet
      
      // Filter to only walkable tiles (not walls, rocks, or water)
      val walkableTiles = startRoomTiles.filterNot { point =>
        dungeon.walls.contains(point) || dungeon.rocks.contains(point) || dungeon.water.contains(point)
      }
      
      // Prefer the center point if it's walkable, otherwise find the closest walkable tile to center
      val centerPoint = Point(
        startRoomX + Dungeon.roomSize / 2,
        startRoomY + Dungeon.roomSize / 2
      )
      
      val dungeonEntranceTile = if (walkableTiles.contains(centerPoint)) {
        centerPoint
      } else {
        // Find the walkable tile closest to the center
        walkableTiles.minBy { tile =>
          val dx = tile.x - centerPoint.x
          val dy = tile.y - centerPoint.y
          dx * dx + dy * dy
        }
      }
      
      println(s"[StartingState] Generating path from player spawn $playerSpawnPoint to dungeon entrance $dungeonEntranceTile")
      println(s"[StartingState] Starting room has ${walkableTiles.size} walkable tiles")
      
      // Collect ALL dungeon tiles as obstacles, EXCEPT the entrance point and its immediate area
      // This ensures the path routes around the dungeon and can only reach it at the entrance
      // We need to allow a small area around the entrance for the path to connect properly
      val entranceArea = (for {
        dx <- -2 to 2
        dy <- -2 to 2
      } yield Point(dungeonEntranceTile.x + dx, dungeonEntranceTile.y + dy)).toSet
      
      val dungeonObstacles = dungeon.tiles.keySet.diff(entranceArea)
      println(s"[StartingState] Avoiding ${dungeonObstacles.size} dungeon tiles (allowing ${entranceArea.size} tile entrance area)")
      
      // Use obstacle-aware pathfinding to navigate around the dungeon
      PathGenerator.generatePathAroundObstacles(
        playerSpawnPoint, 
        dungeonEntranceTile, 
        dungeonObstacles,
        width = 1, 
        worldBounds
      )
    case None => Set.empty
  }
  
  // Combine initial world with player-to-dungeon path
  private lazy val worldMap = {
    val pathTilesMap = playerToDungeonPath.map(p => p -> TileType.Dirt).toMap
    val combinedTiles = initialWorldMap.tiles ++ pathTilesMap
    initialWorldMap.copy(
      tiles = combinedTiles,
      paths = initialWorldMap.paths ++ playerToDungeonPath
    )
  }
  
  println(s"[StartingState] World map finalized with ${worldMap.tiles.size} tiles")
  println(s"[StartingState] Player-to-dungeon path: ${playerToDungeonPath.size} tiles")

  // Generate enemies for dungeon rooms
  private val dungeonRoomsWithDepth: Seq[(Point, Int)] = worldMap.primaryDungeon match {
    case Some(dung) =>
      // Assign depth based on distance from start room
      val startRoom = dung.startPoint
      dung.roomGrid
        .zipWithIndex.map { case (room, idx) =>
          val depth = if (room == startRoom) {
            -1 // Skip starting room - no enemies spawn here
          } else if (room == dung.endpoint.getOrElse(startRoom)) {
            Int.MaxValue // Boss room
          } else if (dung.traderRoom.contains(room)) {
            -1 // Skip trader room
          } else {
            // Calculate depth based on Manhattan distance from start
            math.abs(room.x - startRoom.x) + math.abs(room.y - startRoom.y) + 1
          }
          (room, depth)
        }.filter(_._2 > 0).toSeq // Filter out trader room and starting room
    case None => Seq.empty
  }
  
  private val (enemiesList, spitAbilitiesMap) = dungeonRoomsWithDepth.zipWithIndex.flatMap { 
    case ((room, depth), roomIdx) =>
      if (depth > 0) { // Skip trader rooms and invalid depths
        val (roomEnemies, roomSpitAbilities) = EnemyGeneration.createEnemiesForRoom(room, depth, roomIdx)
        Some((roomEnemies, roomSpitAbilities))
      } else {
        None
      }
  }.unzip

  val enemies: Set[Entity] = enemiesList.flatten.toSet
  val allSpitAbilities: Map[String, Entity] = spitAbilitiesMap.flatten.toMap

  // For backward compatibility, maintain the snakeSpitAbilities val
  val snakeSpitAbilities: Map[Int, Entity] = allSpitAbilities.zipWithIndex.map { case ((k, v), idx) => idx -> v }.toMap

  /**
   * Find a suitable spawn point for the player in the open world (not in dungeon).
   * Tries to find a walkable grass tile away from the dungeon.
   * OPTIMIZED: Caches dungeon bounds and uses efficient tile lookup.
   */
  private def findPlayerSpawnPoint(worldMap: map.WorldMap, bounds: MapBounds): Point = {
    // Convert room bounds to tile bounds
    val (minX, maxX, minY, maxY) = bounds.toTileBounds(Dungeon.roomSize)
    
    // If there's a dungeon, find a spawn point away from it
    worldMap.primaryDungeon match {
      case Some(dungeon) =>
        // OPTIMIZED: Calculate dungeon bounds once
        val dungeonMinX = dungeon.roomGrid.map(_.x).min * Dungeon.roomSize
        val dungeonMaxX = dungeon.roomGrid.map(_.x).max * Dungeon.roomSize + Dungeon.roomSize
        val dungeonMinY = dungeon.roomGrid.map(_.y).min * Dungeon.roomSize
        val dungeonMaxY = dungeon.roomGrid.map(_.y).max * Dungeon.roomSize + Dungeon.roomSize
        
        // Calculate dungeon entrance once
        val entranceTile = Point(
          dungeon.startPoint.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          dungeon.startPoint.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )
        
        // Try to find a suitable spawn point away from dungeon
        val random = new scala.util.Random(worldMap.tiles.size)
        val maxAttempts = 100
        
        // OPTIMIZED: Use tail recursion instead of while loop
        @scala.annotation.tailrec
        def tryFindSpawn(attempt: Int): Point = {
          if (attempt >= maxAttempts) {
            // Fallback: spawn near world edge
            Point(minX + 10, minY + 10)
          } else {
            val x = random.between(minX, maxX)
            val y = random.between(minY, maxY)
            val candidate = Point(x, y)
            
            // OPTIMIZED: Use direct map lookup instead of .get().exists()
            val isWalkable = worldMap.tiles.get(candidate) match {
              case Some(TileType.Grass1 | TileType.Grass2 | TileType.Grass3 | TileType.Dirt) => true
              case _ => false
            }
            
            val isNotInDungeon = x < dungeonMinX || x > dungeonMaxX || 
                                 y < dungeonMinY || y > dungeonMaxY
            
            // OPTIMIZED: Compare squared distances to avoid expensive sqrt operation
            // Minimum distance: 20 tiles → squared = 400 (20^2)
            val dx = candidate.x - entranceTile.x
            val dy = candidate.y - entranceTile.y
            val distanceSquared = dx * dx + dy * dy
            val minDistanceSquared = 400  // 20^2
            
            if (isWalkable && isNotInDungeon && distanceSquared > minDistanceSquared) {
              candidate
            } else {
              tryFindSpawn(attempt + 1)
            }
          }
        }
        
        tryFindSpawn(0)
        
      case None =>
        // No dungeon, spawn at world center
        Point(0, 0)
    }
  }

  val player: Entity = {
    // Spawn player in open world area, not in dungeon
    val playerPos = playerSpawnPoint
    
    // PERFORMANCE FIX: Start with VERY limited sight memory - only immediate surroundings
    // Previous code filtered ALL world tiles which is extremely slow with large worlds
    val initialVisibleRange = 5  // Reduced from 15 - only see immediate surroundings at start
    
    // OPTIMIZED: Calculate bounds once and only check tiles in that range
    val minX = playerPos.x - initialVisibleRange
    val maxX = playerPos.x + initialVisibleRange
    val minY = playerPos.y - initialVisibleRange
    val maxY = playerPos.y + initialVisibleRange
    
    // Only check tiles that could possibly be in range (much faster than filtering all tiles)
    val initiallyVisibleTiles = (for {
      x <- minX to maxX
      y <- minY to maxY
      point = Point(x, y)
      if worldMap.tiles.contains(point)
    } yield point).toSet
    
    println(s"[StartingState] Creating player at position $playerPos with initial sight memory of ${initiallyVisibleTiles.size} nearby tiles (out of ${worldMap.tiles.size} total)")
    val playerEntity = Entity(
      id = "Player ID",
      Movement(position = playerPos),
      EntityTypeComponent(EntityType.Player),
      Health(70),
      Initiative(10),
      Inventory(
        itemEntityIds = (playerStartingItems ++ playerStartingEquipment).map(_.id).toSeq
      ),
      Equipment(
        armor = Some(Equippable.armor(EquipmentSlot.Armor, 1, "Chainmail Armor")),
        weapon = Some(Equippable.weapon(3, "Basic Sword"))
      ),
      SightMemory(seenPoints = initiallyVisibleTiles),  // Only nearby tiles initially
      EventMemory(),
      Drawable(Sprites.playerSprite),
      Hitbox(),
      Experience(),
      Coins(),
      DeathEvents()
    )
    println(s"[StartingState] Player created with ${playerEntity.get[SightMemory].map(_.seenPoints.size).getOrElse(0)} seen points")
    playerEntity
  }

  // Generate items and locked doors from dungeon
  val items: Set[Entity] = worldMap.allItems.map { case (roomPoint, itemRef) =>
    // Convert room coordinates to tile coordinates (center of room)
    val tilePoint = Point(
      roomPoint.x * Dungeon.roomSize + Dungeon.roomSize / 2,
      roomPoint.y * Dungeon.roomSize + Dungeon.roomSize / 2
    )
    val id = s"item-${itemRef.toString}-${tilePoint.x}-${tilePoint.y}"
    val baseEntity = itemRef.createEntity(id)
    // Add Movement component with the item's tile position
    baseEntity.addComponent(Movement(tilePoint))
  }
  
  val lockedDoors: Set[Entity] = worldMap.primaryDungeon.toSeq.flatMap { dungeon =>
    dungeon.roomConnections.filter(_.isLocked).map { connection =>
      // LockedDoor is an EntityType, we need to create an Entity with it
      val lockType = connection.optLock.get
      
      // Position door at the edge of the origin room where it connects
      val originRoomX = connection.originRoom.x * Dungeon.roomSize
      val originRoomY = connection.originRoom.y * Dungeon.roomSize
      
      val doorPoint = connection.direction match {
        case Direction.Up => Point(originRoomX + Dungeon.roomSize / 2, originRoomY)
        case Direction.Down => Point(originRoomX + Dungeon.roomSize / 2, originRoomY + Dungeon.roomSize - 1)
        case Direction.Left => Point(originRoomX, originRoomY + Dungeon.roomSize / 2)
        case Direction.Right => Point(originRoomX + Dungeon.roomSize - 1, originRoomY + Dungeon.roomSize / 2)
      }
      
      Entity(
        id = s"locked-door-${doorPoint.x}-${doorPoint.y}",
        Movement(doorPoint),
        EntityTypeComponent(lockType),
        Drawable(lockType.keyColour match {
          case game.entity.KeyColour.Yellow => data.Sprites.yellowDoorSprite
          case game.entity.KeyColour.Blue => data.Sprites.blueDoorSprite
          case game.entity.KeyColour.Red => data.Sprites.redDoorSprite
        }),
        Hitbox()
      )
    }
  }.toSet

  // Create trader entity if dungeon has trader room
  val trader: Option[Entity] = worldMap.primaryDungeon.flatMap { dungeon =>
    dungeon.traderRoom.map { traderRoom =>
      val traderPos = Point(
        traderRoom.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        traderRoom.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      data.Entities.trader("trader-floor-1", traderPos)
    }
  }

  println(s"[StartingState] Creating GameState with worldMap containing ${worldMap.tiles.size} tiles")
  println(s"[StartingState] Dungeon has ${enemies.size} enemies and ${items.size} items")
  
  val startingGameState: GameState = GameState(
    playerEntityId = player.id,
    entities = Vector(player) ++ playerStartingItems ++ playerStartingEquipment ++ enemies ++ items ++ lockedDoors ++ allSpitAbilities.values ++ trader.toSeq,
    worldMap = worldMap
  )
  
  println(s"[StartingState] GameState created. WorldMap tiles: ${startingGameState.worldMap.tiles.size}, total entities: ${startingGameState.entities.size}")
}
