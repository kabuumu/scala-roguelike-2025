package game

import data.DeathEvents.DeathEventReference.{GiveExperience, SpawnEntity}
import data.Entities.EntityReference.Slimelet
import data.{Enemies, Items, Sprites}
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.entity.Movement.position
import game.system.event.GameSystemEvent.{AddExperienceEvent, SpawnEntityWithCollisionCheckEvent}
import map.{Dungeon, MapGenerator, WorldMapGenerator, WorldMapConfig, WorldConfig, MapBounds, RiverConfig, DungeonConfig}


object StartingState {
  // Generate a simple open world with just grass, dirt, trees, and rivers
  // No dungeons or enemies for now - just exploring the procedural terrain
  private val worldBounds = MapBounds(-50, 50, -50, 50)  // Larger world
  
  private val worldMap = WorldMapGenerator.generateWorldMap(
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
      dungeonConfigs = Seq.empty,  // No dungeons for now
      riverConfigs = Seq(
        RiverConfig(
          startPoint = Point(-500, -400),
          flowDirection = (1, 1),
          length = 400,
          width = 2,
          curviness = 0.3,
          bounds = worldBounds,
          seed = System.currentTimeMillis()
        ),
        RiverConfig(
          startPoint = Point(500, -300),
          flowDirection = (-1, 1),
          length = 350,
          width = 2,
          curviness = 0.25,
          bounds = worldBounds,
          seed = System.currentTimeMillis() + 1
        )
      ),
      generatePathsToDungeons = false,
      generatePathsBetweenDungeons = false,
      pathsPerDungeon = 0,
      pathWidth = 1,
      minDungeonSpacing = 10
    )
  )
  
  // Create a simple dummy dungeon for compatibility with existing game state
  // This is just a placeholder - the player spawns in the open world
  val dungeon: Dungeon = Dungeon(
    roomGrid = Set(Point(0, 0)),
    roomConnections = Set.empty,
    startPoint = Point(0, 0),
    outdoorRooms = Set(Point(0, 0)),  // Entire "dungeon" is outdoor
    seed = System.currentTimeMillis()
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

  // No enemies for the simple open world demo
  val enemies: Set[Entity] = Set.empty
  val allSpitAbilities: Map[String, Entity] = Map.empty

  // For backward compatibility, maintain the snakeSpitAbilities val
  val snakeSpitAbilities: Map[Int, Entity] = Map.empty

  val player: Entity = {
    // Spawn player in the center of the open world
    val playerEntity = Entity(
      id = "Player ID",
      Movement(position = Point(0, 0)),  // Center of the world
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
      SightMemory(seenPoints = worldMap.tiles.keySet),  // Can see entire open world
      EventMemory(),
      Drawable(Sprites.playerSprite),
      Hitbox(),
      Experience(),
      Coins(),
      DeathEvents()
    )
    playerEntity
  }

  // No trader, items, or locked doors in the simple open world
  val items: Set[Entity] = Set.empty
  val lockedDoors: Set[Entity] = Set.empty

  val startingGameState: GameState = GameState(
    playerEntityId = player.id,
    entities = Vector(player) ++ playerStartingItems ++ playerStartingEquipment,
    dungeon = dungeon,
    worldTiles = Some(worldMap.tiles)
  )
}
