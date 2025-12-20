package game

import data.{Enemies, Items, Sprites}
import game.entity.*
import map.*

object StartingState {

  // Helpers for items/equipment (kept as vals since they are definitions)
  val playerStartingItems: Set[Entity] = Set(
    Items.healingPotion("player-potion-1"),
    Items.healingPotion("player-potion-2"),
    Items.fireballScroll("player-scroll-1"),
    Items.fireballScroll("player-scroll-2"),
    Items.bow("player-bow-1")
  ) ++ (1 to 6).map(i => Items.arrow(s"player-arrow-$i"))

  val playerStartingEquipment: Set[Entity] = Set(
    Items.basicSword("player-starting-sword"),
    Items.chainmailArmor("player-starting-armor")
  )

  /** Generates a fresh GameState for Adventure mode. Starts in the village
    * (0,0).
    */
  def startAdventure(): GameState = {
    // Generate standard open world
    val worldSize = 10
    val worldBounds = MapBounds(-worldSize, worldSize, -worldSize, worldSize)

    val worldMap = WorldMapGenerator.generateWorldMap(
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

    // Player starts at (0,0) in Adventure mode (Village)
    val playerSpawnPoint = Point(0, 0)

    createGameState(
      worldMap,
      playerSpawnPoint,
      GameMode.Adventure,
      dungeonFloor = 0
    )
  }

  /** Generates a fresh GameState for Gauntlet mode. Starts in a dungeon.
    */
  def startGauntlet(): GameState = {
    // For Gauntlet mode, we want a dungeon-only world
    // We'll generate a single dungeon and wrap it in a WorldMap
    val dungeonSeed = System.currentTimeMillis()
    val worldMap = createGauntletWorldMap(dungeonFloor = 1, seed = dungeonSeed)

    // Find a dungeon to start in (there is only one)
    val dungeon = worldMap.dungeons.head
    val playerSpawnPoint = Point(
      dungeon.startPoint.x * Dungeon.roomSize + Dungeon.roomSize / 2,
      dungeon.startPoint.y * Dungeon.roomSize + Dungeon.roomSize / 2
    )

    createGameState(
      worldMap,
      playerSpawnPoint,
      GameMode.Gauntlet,
      dungeonFloor = 1
    )
  }

  /** Helper to create a world map that contains ONLY a single dungeon. Used for
    * Gauntlet mode.
    */
  def createGauntletWorldMap(dungeonFloor: Int, seed: Long): WorldMap = {
    // Define bounds for the dungeon
    val size = 20 // 20x20 grid for dungeon generation
    val bounds = MapBounds(-size, size, -size, size)

    // Generate the dungeon with strict limits to ensure performance/reliability
    val dungeonConfig = DungeonConfig(
      bounds = bounds,
      seed = seed,
      explicitSize = Some(
        30 + math.min(dungeonFloor * 2, 20)
      ) // Start at 30, cap at +20 (50 rooms)
    )
    val dungeon = DungeonGenerator.generateDungeon(dungeonConfig)

    // Create a WorldMap wrapping just this dungeon
    WorldMap(
      tiles = dungeon.tiles, // Only dungeon tiles
      dungeons = Seq(dungeon),
      rivers = Set.empty,
      paths = Set.empty,
      bridges = Set.empty,
      bounds = bounds // Restrict map bounds to dungeon area
    )
  }

  // Backward compatibility
  def startingGameState: GameState = startAdventure()

  private def createGameState(
      worldMap: WorldMap,
      playerSpawnPoint: Point,
      gameMode: GameMode,
      dungeonFloor: Int
  ): GameState = {
    // Generate enemies, items, etc. based on the world map

    // Generate enemies for ALL dungeon rooms
    val dungeonRoomsWithDepth: Seq[(Point, Int)] = worldMap.dungeons.flatMap {
      dung =>
        val startRoom = dung.startPoint
        dung.roomGrid.zipWithIndex
          .map { case (room, idx) =>
            val depth = if (room == startRoom) {
              -1
            } else if (room == dung.endpoint.getOrElse(startRoom)) {
              Int.MaxValue
            } else if (dung.traderRoom.contains(room)) {
              -1
            } else {
              math.abs(room.x - startRoom.x) + math.abs(
                room.y - startRoom.y
              ) + 1
            }
            (room, depth)
          }
          .filter(_._2 > 0)
    }.toSeq

    val (enemiesList, spitAbilitiesMap) =
      dungeonRoomsWithDepth.zipWithIndex.flatMap {
        case ((room, depth), roomIdx) =>
          if (depth > 0) {
            val (roomEnemies, roomSpitAbilities) =
              EnemyGeneration.createEnemiesForRoom(
                room,
                depth,
                roomIdx,
                worldMap
              )
            Some((roomEnemies, roomSpitAbilities))
          } else {
            None
          }
      }.unzip

    val enemies: Set[Entity] = enemiesList.flatten.toSet
    val allSpitAbilities: Map[String, Entity] = spitAbilitiesMap.flatten.toMap

    // Create Player Entity
    val initialVisibleRange = 5
    val minX = playerSpawnPoint.x - initialVisibleRange
    val maxX = playerSpawnPoint.x + initialVisibleRange
    val minY = playerSpawnPoint.y - initialVisibleRange
    val maxY = playerSpawnPoint.y + initialVisibleRange

    val initiallyVisibleTiles = (for {
      x <- minX to maxX
      y <- minY to maxY
      point = Point(x, y)
      if worldMap.tiles.contains(point)
    } yield point).toSet

    val playerEntity = Entity(
      id = "Player ID",
      Movement(position = playerSpawnPoint),
      EntityTypeComponent(EntityType.Player),
      Health(70),
      Initiative(10),
      Inventory(
        itemEntityIds =
          (playerStartingItems ++ playerStartingEquipment).map(_.id).toSeq
      ),
      Equipment(
        armor =
          Some(Equippable.armor(EquipmentSlot.Armor, 1, "Chainmail Armor")),
        weapon = Some(Equippable.weapon(3, "Basic Sword"))
      ),
      SightMemory(seenPoints = initiallyVisibleTiles),
      EventMemory(),
      Drawable(Sprites.playerSprite),
      Hitbox(),
      Experience(),
      Coins(),
      DeathEvents()
    )

    // Generate items
    val items: Set[Entity] = worldMap.allItems.map {
      case (roomPoint, itemRef) =>
        val tilePoint = Point(
          roomPoint.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          roomPoint.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )
        val id = s"item-${itemRef.toString}-${tilePoint.x}-${tilePoint.y}"
        val baseEntity = itemRef.createEntity(id)
        baseEntity.addComponent(Movement(tilePoint))
    }

    // Create locked doors
    val lockedDoors: Set[Entity] = worldMap.dungeons.flatMap { dungeon =>
      dungeon.roomConnections.filter(_.isLocked).map { connection =>
        val lockType = connection.optLock.get
        val originRoomX = connection.originRoom.x * Dungeon.roomSize
        val originRoomY = connection.originRoom.y * Dungeon.roomSize

        val doorPoint = connection.direction match {
          case Direction.Up =>
            Point(originRoomX + Dungeon.roomSize / 2, originRoomY)
          case Direction.Down =>
            Point(
              originRoomX + Dungeon.roomSize / 2,
              originRoomY + Dungeon.roomSize
            )
          case Direction.Left =>
            Point(originRoomX, originRoomY + Dungeon.roomSize / 2)
          case Direction.Right =>
            Point(
              originRoomX + Dungeon.roomSize,
              originRoomY + Dungeon.roomSize / 2
            )
        }

        Entity(
          id = s"locked-door-${doorPoint.x}-${doorPoint.y}",
          Movement(doorPoint),
          EntityTypeComponent(lockType),
          Drawable(lockType.keyColour match {
            case game.entity.KeyColour.Yellow => data.Sprites.yellowDoorSprite
            case game.entity.KeyColour.Blue   => data.Sprites.blueDoorSprite
            case game.entity.KeyColour.Red    => data.Sprites.redDoorSprite
          }),
          Hitbox()
        )
      }
    }.toSet

    // Dungeon Traders
    val dungeonTraders: Seq[Entity] = worldMap.dungeons.zipWithIndex.flatMap {
      case (dungeon, dungeonIdx) =>
        dungeon.traderRoom.map { traderRoom =>
          val traderPos = Point(
            traderRoom.x * Dungeon.roomSize + Dungeon.roomSize / 2,
            traderRoom.y * Dungeon.roomSize + Dungeon.roomSize / 2
          )
          data.Entities.trader(s"trader-dungeon-$dungeonIdx", traderPos)
        }
    }

    // Village Traders (Only if not spawning in a building that contains spawn point - simplified here)
    val villageTraders: Seq[Entity] = worldMap.villages.zipWithIndex.flatMap {
      case (village, villageIdx) =>
        village.buildings.zipWithIndex.flatMap { case (building, buildingIdx) =>
          val (minBounds, maxBounds) = building.bounds
          val containsPlayerSpawn =
            playerSpawnPoint.x >= minBounds.x && playerSpawnPoint.x <= maxBounds.x &&
              playerSpawnPoint.y >= minBounds.y && playerSpawnPoint.y <= maxBounds.y

          if (!containsPlayerSpawn) {
            val id = s"npc-village-$villageIdx-building-$buildingIdx"
            val position = building.centerTile
            val entity = building.buildingType match {
              case map.BuildingType.Healer => data.Entities.healer(id, position)
              case map.BuildingType.PotionShop =>
                data.Entities.potionMerchant(id, position)
              case map.BuildingType.EquipmentShop =>
                data.Entities.equipmentMerchant(id, position)
              case map.BuildingType.Generic =>
                data.Entities.villager(id, position)
            }
            Some(entity)
          } else {
            None
          }
        }
    }

    GameState(
      playerEntityId = playerEntity.id,
      entities = Vector(
        playerEntity
      ) ++ playerStartingItems ++ playerStartingEquipment ++ enemies ++ items ++ lockedDoors ++ allSpitAbilities.values ++ dungeonTraders ++ villageTraders,
      worldMap = worldMap,
      dungeonFloor = dungeonFloor,
      gameMode = gameMode
    )
  }

  object EnemyGeneration {
    import Enemies.EnemyDifficulty.*
    import Enemies.EnemyReference

    case class EnemyGroup(enemies: Seq[EnemyReference])

    def enemiesForDepth(depth: Int): EnemyGroup = depth match {
      case d if d == Int.MaxValue => EnemyGroup(Seq(EnemyReference.Boss))
      case 1                      => EnemyGroup(Seq(EnemyReference.Slimelet))
      case 2                      =>
        EnemyGroup(Seq(EnemyReference.Slimelet, EnemyReference.Slimelet))
      case 3 => EnemyGroup(Seq(EnemyReference.Slime))
      case 4 => EnemyGroup(Seq(EnemyReference.Slime, EnemyReference.Slimelet))
      case 5 => EnemyGroup(Seq(EnemyReference.Rat))
      case 6 => EnemyGroup(Seq(EnemyReference.Snake))
      case d if d >= 7 && d % 2 == 1 =>
        EnemyGroup(Seq(EnemyReference.Rat, EnemyReference.Rat))
      case d if d >= 8 && d % 2 == 0 =>
        EnemyGroup(Seq(EnemyReference.Snake, EnemyReference.Snake))
      case _ => EnemyGroup(Seq(EnemyReference.Slimelet))
    }

    def findWalkableTilesInRoom(
        roomPoint: Point,
        worldMap: map.WorldMap
    ): Seq[Point] = {
      val roomMinX = roomPoint.x * Dungeon.roomSize
      val roomMaxX = roomMinX + Dungeon.roomSize
      val roomMinY = roomPoint.y * Dungeon.roomSize
      val roomMaxY = roomMinY + Dungeon.roomSize

      val roomTiles = for {
        x <- roomMinX to roomMaxX
        y <- roomMinY to roomMaxY
        point = Point(x, y)
        if worldMap.tiles.contains(point)
      } yield point

      roomTiles.filterNot { point =>
        worldMap.walls.contains(point) || worldMap.rocks.contains(
          point
        ) || worldMap.water.contains(point)
      }.toSeq
    }

    def createEnemiesForRoom(
        roomPoint: Point,
        depth: Int,
        roomIndex: Int,
        worldMap: map.WorldMap
    ): (Seq[Entity], Map[String, Entity]) = {
      val roomCenter = Point(
        roomPoint.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        roomPoint.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )

      val walkableTiles = findWalkableTilesInRoom(roomPoint, worldMap)

      if (walkableTiles.isEmpty) {
        return (Seq.empty, Map.empty)
      }

      val centerPosition = if (walkableTiles.contains(roomCenter)) {
        roomCenter
      } else {
        walkableTiles.minBy { tile =>
          val dx = tile.x - roomCenter.x
          val dy = tile.y - roomCenter.y
          dx * dx + dy * dy
        }
      }

      val enemyGroup = enemiesForDepth(depth)
      val enemies = enemyGroup.enemies.zipWithIndex.map {
        case (enemyRef, enemyIndex) =>
          val enemyId = s"${enemyRef.toString}-R$roomIndex-$enemyIndex"

          val position = if (enemyIndex == 0) {
            centerPosition
          } else {
            val offsets = Seq(
              (1, 0),
              (-1, 0),
              (0, 1),
              (0, -1),
              (1, 1),
              (-1, -1),
              (1, -1),
              (-1, 1),
              (2, 0),
              (-2, 0),
              (0, 2),
              (0, -2)
            )

            val candidatePositions = offsets
              .map { case (dx, dy) =>
                Point(
                  centerPosition.x + dx * enemyIndex,
                  centerPosition.y + dy * enemyIndex
                )
              }
              .filter(walkableTiles.contains)

            candidatePositions.headOption.getOrElse {
              walkableTiles((enemyIndex * 13) % walkableTiles.size)
            }
          }

          enemyRef match {
            case EnemyReference.Rat   => Enemies.rat(enemyId, position)
            case EnemyReference.Snake =>
              val spitId = s"$enemyId-spit"
              Enemies.snake(enemyId, position, spitId)
            case EnemyReference.Slime    => Enemies.slime(enemyId, position)
            case EnemyReference.Slimelet => Enemies.slimelet(enemyId, position)
            case EnemyReference.Boss     =>
              val bossBlastId = s"$enemyId-blast"
              Enemies.boss(enemyId, position, bossBlastId)
          }
      }

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
}
