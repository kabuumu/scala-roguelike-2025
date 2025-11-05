package game

import data.{Enemies, Items, Sprites}
import game.entity.*
import map.*


object StartingState {
  // Generate an open world with grass, dirt, trees, rivers, and dungeons
  // World size: 21x21 room grid (-10 to 10) to allow multiple dungeon generation
  // For a 21x21 world, we generate 4 dungeons positioned in quadrants
  val worldSize = 10  // Room radius (full grid is 2*worldSize+1 = 21x21 rooms)
  
  private val worldBounds = MapBounds(-worldSize, worldSize, -worldSize, worldSize)  // Moderate world size
  
  println(s"[StartingState] Generating world map with bounds: $worldBounds")
  
  // Calculate dungeon entrance side based on where we want the dungeon relative to spawn
  // Place dungeon in a corner/edge of the world, and orient entrance toward center
  // This ensures the player can approach from the open world side
  private val dungeonEntranceSide: Direction = {
    // Place dungeon in bottom-right area, entrance facing left (toward player spawn)
    Direction.Left
  }
  
  println(s"[StartingState] Dungeon entrance side: $dungeonEntranceSide")
  
  // PERFORMANCE: Make world generation lazy to avoid computation until needed
  // Dungeons are now calculated automatically based on world size
  // For a 21x21 world (-10 to 10), this will create 4 dungeons
  private lazy val worldMap = WorldMapGenerator.generateWorldMap(
    WorldMapConfig(
      worldConfig = WorldConfig(
        bounds = worldBounds,
        grassDensity = 0.65,
        treeDensity = 0.20,
        dirtDensity = 0.10,
        ensureWalkablePaths = true,
        perimeterTrees = true,
        seed = System.currentTimeMillis()
      )
    )
  )
  
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
    import Enemies.EnemyDifficulty.*
    import Enemies.EnemyReference
    
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
     * Find walkable tiles in a room (tiles that are not walls, rocks, or water).
     */
    def findWalkableTilesInRoom(roomPoint: Point, worldMap: map.WorldMap): Seq[Point] = {
      val roomMinX = roomPoint.x * Dungeon.roomSize
      val roomMaxX = roomMinX + Dungeon.roomSize
      val roomMinY = roomPoint.y * Dungeon.roomSize
      val roomMaxY = roomMinY + Dungeon.roomSize
      
      // Generate all tiles within the room bounds (inclusive to cover room perimeter)
      val roomTiles = for {
        x <- roomMinX to roomMaxX
        y <- roomMinY to roomMaxY
        point = Point(x, y)
        if worldMap.tiles.contains(point)
      } yield point
      
      // Filter out non-walkable tiles (walls, rocks, water)
      roomTiles.filterNot { point =>
        worldMap.walls.contains(point) || worldMap.rocks.contains(point) || worldMap.water.contains(point)
      }.toSeq
    }
    
    /**
     * Create enemy entities for a room based on its depth and position.
     * Ensures enemies spawn on walkable tiles only.
     */
    def createEnemiesForRoom(roomPoint: Point, depth: Int, roomIndex: Int, worldMap: map.WorldMap): (Seq[Entity], Map[String, Entity]) = {
      val roomCenter = Point(
        roomPoint.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        roomPoint.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      
      // Find all walkable tiles in the room
      val walkableTiles = findWalkableTilesInRoom(roomPoint, worldMap)
      
      if (walkableTiles.isEmpty) {
        // If no walkable tiles (should not happen in a well-formed dungeon), return empty
        return (Seq.empty, Map.empty)
      }
      
      // Prefer room center if it's walkable, otherwise use closest walkable tile
      val centerPosition = if (walkableTiles.contains(roomCenter)) {
        roomCenter
      } else {
        // Find the walkable tile closest to the room center
        walkableTiles.minBy { tile =>
          val dx = tile.x - roomCenter.x
          val dy = tile.y - roomCenter.y
          dx * dx + dy * dy
        }
      }
      
      val enemyGroup = enemiesForDepth(depth)
      val enemies = enemyGroup.enemies.zipWithIndex.map { case (enemyRef, enemyIndex) =>
        val enemyId = s"${enemyRef.toString}-R$roomIndex-$enemyIndex"
        
        // Find a walkable position for this enemy
        val position = if (enemyIndex == 0) {
          centerPosition
        } else {
          // Try to find a walkable tile near the center
          // Check positions in a spiral pattern around center
          val offsets = Seq(
            (1, 0), (-1, 0), (0, 1), (0, -1),
            (1, 1), (-1, -1), (1, -1), (-1, 1),
            (2, 0), (-2, 0), (0, 2), (0, -2)
          )
          
          val candidatePositions = offsets.map { case (dx, dy) =>
            Point(centerPosition.x + dx * enemyIndex, centerPosition.y + dy * enemyIndex)
          }.filter(walkableTiles.contains)
          
          candidatePositions.headOption.getOrElse {
            // If no nearby position is available, distribute enemies across walkable tiles
            // Use prime number 13 to avoid clustering when multiple enemies spawn
            walkableTiles((enemyIndex * 13) % walkableTiles.size)
          }
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

  // Determine player spawn point - spawn player in first village building
  private lazy val playerSpawnPoint = worldMap.playerSpawnPoint
  
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
        val (roomEnemies, roomSpitAbilities) = EnemyGeneration.createEnemiesForRoom(room, depth, roomIdx, worldMap)
        Some((roomEnemies, roomSpitAbilities))
      } else {
        None
      }
  }.unzip

  val enemies: Set[Entity] = enemiesList.flatten.toSet
  val allSpitAbilities: Map[String, Entity] = spitAbilitiesMap.flatten.toMap

  // For backward compatibility, maintain the snakeSpitAbilities val
  val snakeSpitAbilities: Map[Int, Entity] = allSpitAbilities.zipWithIndex.map { case ((k, v), idx) => idx -> v }.toMap
  

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
        case Direction.Down => Point(originRoomX + Dungeon.roomSize / 2, originRoomY + Dungeon.roomSize)
        case Direction.Left => Point(originRoomX, originRoomY + Dungeon.roomSize / 2)
        case Direction.Right => Point(originRoomX + Dungeon.roomSize, originRoomY + Dungeon.roomSize / 2)
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
  val dungeonTrader: Option[Entity] = worldMap.primaryDungeon.flatMap { dungeon =>
    dungeon.traderRoom.map { traderRoom =>
      val traderPos = Point(
        traderRoom.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        traderRoom.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      data.Entities.trader("trader-floor-1", traderPos)
    }
  }

  // Create trader entity in shop
  val shopTrader: Option[Entity] = worldMap.shop.map { shop =>
    data.Entities.trader("trader-shop", shop.centerTile)
  }

  println(s"[StartingState] Creating GameState with worldMap containing ${worldMap.tiles.size} tiles")
  println(s"[StartingState] Dungeon has ${enemies.size} enemies and ${items.size} items")
  println(s"[StartingState] Shop trader: ${shopTrader.map(_ => "present").getOrElse("absent")}")
  
  val startingGameState: GameState = GameState(
    playerEntityId = player.id,
    entities = Vector(player) ++ playerStartingItems ++ playerStartingEquipment ++ enemies ++ items ++ lockedDoors ++ allSpitAbilities.values ++ dungeonTrader.toSeq ++ shopTrader.toSeq,
    worldMap = worldMap
  )
  
  println(s"[StartingState] GameState created. WorldMap tiles: ${startingGameState.worldMap.tiles.size}, total entities: ${startingGameState.entities.size}")
}
