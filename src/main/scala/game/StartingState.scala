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
  // World size optimized for performance (20x20 rooms = ~45k tiles)
  private val worldBounds = MapBounds(-10, 10, -10, 10)  // Moderate world size
  
  println(s"[StartingState] Generating world map with bounds: $worldBounds")
  
  // First generate world without paths (we'll add player-to-dungeon path after player spawn is determined)
  private val initialWorldMap = WorldMapGenerator.generateWorldMap(
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
        DungeonConfig(
          bounds = Some(worldBounds),
          entranceSide = Direction.Up,
          size = 20,
          lockedDoorCount = 3,
          itemCount = 6,
          seed = System.currentTimeMillis()
        )
      ),
      riverConfigs = Seq(
        // Rivers flow through the center of the world so they're visible
        // World bounds in tiles: (-100, 110) x (-100, 110)
        RiverConfig(
          startPoint = Point(-60, -80),    // Start from upper left area
          flowDirection = (1, 1),           // Flow diagonally down-right
          length = 120,                     // Long river
          width = 2,                        // Visible width
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
  private val playerSpawnPoint = findPlayerSpawnPoint(initialWorldMap, worldBounds)
  
  // Generate path from player spawn to dungeon entrance
  private val playerToDungeonPath: Set[Point] = initialWorldMap.primaryDungeon match {
    case Some(dungeon) =>
      val dungeonEntranceTile = Point(
        dungeon.startPoint.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        dungeon.startPoint.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      println(s"[StartingState] Generating path from player spawn $playerSpawnPoint to dungeon entrance $dungeonEntranceTile")
      PathGenerator.generatePath(playerSpawnPoint, dungeonEntranceTile, width = 1, worldBounds)
    case None => Set.empty
  }
  
  // Combine initial world with player-to-dungeon path
  private val worldMap = {
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
      dung.roomGrid.zipWithIndex.map { case (room, idx) =>
        val depth = if (room == dung.endpoint.getOrElse(startRoom)) {
          Int.MaxValue // Boss room
        } else if (dung.traderRoom.contains(room)) {
          -1 // Skip trader room
        } else {
          // Calculate depth based on Manhattan distance from start
          math.abs(room.x - startRoom.x) + math.abs(room.y - startRoom.y) + 1
        }
        (room, depth)
      }.filter(_._2 > 0).toSeq // Filter out trader room
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
   */
  private def findPlayerSpawnPoint(worldMap: map.WorldMap, bounds: MapBounds): Point = {
    // Convert room bounds to tile bounds
    val (minX, maxX, minY, maxY) = bounds.toTileBounds(Dungeon.roomSize)
    
    // If there's a dungeon, find a spawn point away from it
    worldMap.primaryDungeon match {
      case Some(dungeon) =>
        // Get dungeon bounds (in tile coordinates)
        val dungeonRoomPoints = dungeon.roomGrid
        val dungeonMinX = dungeonRoomPoints.map(_.x).min * Dungeon.roomSize
        val dungeonMaxX = dungeonRoomPoints.map(_.x).max * Dungeon.roomSize + Dungeon.roomSize
        val dungeonMinY = dungeonRoomPoints.map(_.y).min * Dungeon.roomSize
        val dungeonMaxY = dungeonRoomPoints.map(_.y).max * Dungeon.roomSize + Dungeon.roomSize
        
        // Try to find a suitable spawn point away from dungeon
        val random = new scala.util.Random(worldMap.tiles.size)
        val maxAttempts = 100
        var attempt = 0
        
        while (attempt < maxAttempts) {
          val x = random.between(minX, maxX)
          val y = random.between(minY, maxY)
          val candidate = Point(x, y)
          
          // Check if this is a walkable tile (grass or dirt) and not in dungeon area
          val isWalkable = worldMap.tiles.get(candidate).exists { tileType =>
            tileType == TileType.Grass1 || tileType == TileType.Grass2 || 
            tileType == TileType.Grass3 || tileType == TileType.Dirt
          }
          
          val isNotInDungeon = !(x >= dungeonMinX && x <= dungeonMaxX && 
                                  y >= dungeonMinY && y <= dungeonMaxY)
          
          // Check it's reasonably far from dungeon entrance
          val distanceFromDungeon = {
            val entranceTile = Point(
              dungeon.startPoint.x * Dungeon.roomSize + Dungeon.roomSize / 2,
              dungeon.startPoint.y * Dungeon.roomSize + Dungeon.roomSize / 2
            )
            val dx = candidate.x - entranceTile.x
            val dy = candidate.y - entranceTile.y
            math.sqrt(dx * dx + dy * dy)
          }
          
          if (isWalkable && isNotInDungeon && distanceFromDungeon > 20) {
            return candidate
          }
          
          attempt += 1
        }
        
        // Fallback: spawn near world edge
        Point(minX + 10, minY + 10)
        
      case None =>
        // No dungeon, spawn at world center
        Point(0, 0)
    }
  }

  val player: Entity = {
    // Spawn player in open world area, not in dungeon
    val playerPos = playerSpawnPoint
    
    // Start with limited sight memory - tiles will be discovered as player explores
    val initialVisibleRange = 15  // Player can initially see 15 tiles in each direction
    val initiallyVisibleTiles = worldMap.tiles.keys.filter { tilePos =>
      math.abs(tilePos.x - playerPos.x) <= initialVisibleRange &&
      math.abs(tilePos.y - playerPos.y) <= initialVisibleRange
    }.toSet
    
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
  val items: Set[Entity] = worldMap.allItems.map { case (point, itemRef) =>
    val id = s"item-${itemRef.toString}-${point.x}-${point.y}"
    itemRef.createEntity(id)
  }
  
  val lockedDoors: Set[Entity] = worldMap.primaryDungeon.toSeq.flatMap { dungeon =>
    dungeon.roomConnections.filter(_.isLocked).map { connection =>
      // LockedDoor is an EntityType, we need to create an Entity with it
      val lockType = connection.optLock.get
      val doorPoint = Point(
        connection.destinationRoom.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        connection.destinationRoom.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
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

  println(s"[StartingState] Creating GameState with worldMap containing ${worldMap.tiles.size} tiles")
  println(s"[StartingState] Dungeon has ${enemies.size} enemies and ${items.size} items")
  
  val startingGameState: GameState = GameState(
    playerEntityId = player.id,
    entities = Vector(player) ++ playerStartingItems ++ playerStartingEquipment ++ enemies ++ items ++ lockedDoors ++ allSpitAbilities.values,
    worldMap = worldMap
  )
  
  println(s"[StartingState] GameState created. WorldMap tiles: ${startingGameState.worldMap.tiles.size}, total entities: ${startingGameState.entities.size}")
}
