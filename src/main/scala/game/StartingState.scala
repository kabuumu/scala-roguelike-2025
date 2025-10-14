package game

import data.DeathEvents.DeathEventReference.{GiveExperience, SpawnEntity}
import data.Entities.EntityReference.Slimelet
import data.{Enemies, Items, Sprites}
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.entity.Movement.position
import game.system.event.GameSystemEvent.{AddExperienceEvent, SpawnEntityWithCollisionCheckEvent}
import map.{Dungeon, MapGenerator}


object StartingState {
  val dungeon: Dungeon = MapGenerator.generateDungeon(dungeonSize = 20, lockedDoorCount = 3, itemCount = 6)

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

  // Generate enemies using the new depth-based system
  val (enemies, allSpitAbilities) = {
    val nonStartRooms = dungeon.roomGrid - dungeon.startPoint
    val roomDepths = dungeon.roomDepths
    
    val enemiesAndAbilities = nonStartRooms.zipWithIndex.map { case (roomPoint, index) =>
      // If this is the endpoint room and we have a boss room, place boss instead of regular enemies
      if (dungeon.hasBossRoom && dungeon.endpoint.contains(roomPoint)) {
        // Place boss in endpoint room
        EnemyGeneration.createEnemiesForRoom(roomPoint, Int.MaxValue, index) // Use max depth to trigger boss placement
      } else {
        val depth = roomDepths.getOrElse(roomPoint, 1) // Default to depth 1 if not found
        EnemyGeneration.createEnemiesForRoom(roomPoint, depth, index)
      }
    }
    
    val allEnemies = enemiesAndAbilities.flatMap(_._1)
    val combinedAbilities = enemiesAndAbilities.flatMap(_._2).toMap
    
    (allEnemies.toSet, combinedAbilities)
  }

  // For backward compatibility, maintain the snakeSpitAbilities val
  val snakeSpitAbilities: Map[Int, Entity] = allSpitAbilities.values.zipWithIndex.map {
    case (ability, index) => index -> ability
  }.toMap

  val player: Entity = dungeon.startPoint match {
    case point =>
      Entity(
        id = "Player ID",
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
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
        SightMemory(),
        EventMemory(),
        Drawable(Sprites.playerSprite),
        Hitbox(),
        Experience(),
        Coins(),
        DeathEvents()
      )
  }

  // Spawn a trader in a separate room adjacent to starting room
  val trader: Entity = {
    // Find a room adjacent to the start point that is not the endpoint or a blocked room
    val adjacentRooms = Direction.values.map(dir => dungeon.startPoint + dir)
      .filter(room => dungeon.roomGrid.contains(room))
      .filterNot(room => dungeon.endpoint.contains(room))
      .filterNot(room => dungeon.blockedRooms.contains(room))
    
    val traderRoom = adjacentRooms.headOption.getOrElse(dungeon.startPoint)
    
    val traderPos = Point(
      traderRoom.x * Dungeon.roomSize + Dungeon.roomSize / 2,
      traderRoom.y * Dungeon.roomSize + Dungeon.roomSize / 2
    )
    data.Entities.trader("trader-1", traderPos)
  }

  val items: Set[Entity] = dungeon.items.zipWithIndex.map {
    case ((point, itemReference), index) =>
      val basePosition = Point(
        point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        point.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      
      // Create entity from reference and place in world
      val itemEntity = itemReference.createEntity(s"item-$index")
      val placedEntity = itemEntity.addComponent(Movement(position = basePosition))
      
      // Add EntityTypeComponent for keys
      itemReference match {
        case data.Items.ItemReference.YellowKey => 
          placedEntity.addComponent(EntityTypeComponent(EntityType.Key(KeyColour.Yellow)))
        case data.Items.ItemReference.BlueKey => 
          placedEntity.addComponent(EntityTypeComponent(EntityType.Key(KeyColour.Blue)))
        case data.Items.ItemReference.RedKey => 
          placedEntity.addComponent(EntityTypeComponent(EntityType.Key(KeyColour.Red)))
        case _ =>
          placedEntity
      }
  }.toSet

  val lockedDoors: Set[Entity] = dungeon.lockedDoors.map {
    case (point, lockedDoor) =>
      Entity(
        Movement(position = Point(
          point.x,
          point.y
        )),
        EntityTypeComponent(lockedDoor),
        Hitbox(),
        lockedDoor.keyColour match {
          case KeyColour.Yellow => Drawable(Sprites.yellowDoorSprite)
          case KeyColour.Blue => Drawable(Sprites.blueDoorSprite)
          case KeyColour.Red => Drawable(Sprites.redDoorSprite)
        }
      )
  }

  val startingGameState: GameState = GameState(
    playerEntityId = player.id,
    entities = Vector(player) ++ playerStartingItems ++ playerStartingEquipment ++ items ++ enemies ++ lockedDoors ++ snakeSpitAbilities.values :+ trader,
    dungeon = dungeon
  )
}
