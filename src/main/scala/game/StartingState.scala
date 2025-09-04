package game

import data.Sprites
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.entity.Movement.position
import game.system.event.GameSystemEvent.{AddExperienceEvent, SpawnEntityEvent}
import map.{Dungeon, MapGenerator, ItemDescriptor}


object StartingState {
  val dungeon: Dungeon = MapGenerator.generateDungeon(dungeonSize = 20, lockedDoorCount = 3, itemCount = 6)

  // Helper function to create slimelets when a slime dies
  private def createSlimelets(deathDetails: DeathDetails): Seq[SpawnEntityEvent] = {
    val slimePosition = deathDetails.victim.position
    val availablePositions = Seq(Direction.Up, Direction.Down, Direction.Left, Direction.Right)
      .map(dir => slimePosition + Direction.asPoint(dir))
    
    // Create 2 slimelets at available adjacent positions
    val slimeletPositions = availablePositions.take(2)
    slimeletPositions.zipWithIndex.map { case (position, index) =>
      val slimeletId = s"Slimelet-${System.currentTimeMillis()}-$index"
      
      val slimelet = Entity(
        id = slimeletId,
        Movement(position = position),
        EntityTypeComponent(EntityType.Enemy),
        Health(10),
        Initiative(8),
        Inventory(Nil, None), // No weapon for slimelets, they use default 1 damage
        Drawable(Sprites.slimeletSprite),
        Hitbox(),
        DeathEvents(deathDetails => deathDetails.killerId.map {
          killerId => AddExperienceEvent(killerId, experienceForLevel(1) / 4)
        }.toSeq)
      )
      
      SpawnEntityEvent(slimelet)
    }
  }

  // Create player's starting inventory items as entities
  val playerStartingItems: Set[Entity] = Set(
    ItemFactory.createPotion("player-potion-1"),
    ItemFactory.createPotion("player-potion-2"),
    ItemFactory.createScroll("player-scroll-1"),
    ItemFactory.createScroll("player-scroll-2"),
    ItemFactory.createBow("player-bow-1")
  ) ++ (1 to 6).map(i => ItemFactory.createArrow(s"player-arrow-$i"))
  
  // Create weapons as entities for enemies and player
  val ratWeapons: Map[Int, Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.collect {
    case (_, index) if index % 3 == 0 => index -> ItemFactory.createWeapon(s"rat-weapon-$index", 8, Melee)
  }.toMap
  
  val snakeWeapons: Map[Int, Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.collect {
    case (_, index) if index % 3 == 1 => index -> ItemFactory.createWeapon(s"snake-weapon-$index", 6, Ranged(4))
  }.toMap
  
  val slimeWeapons: Map[Int, Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.collect {
    case (_, index) if index % 3 == 2 => index -> ItemFactory.createWeapon(s"slime-weapon-$index", 6, Melee)
  }.toMap
  
  val playerPrimaryWeapon: Entity = ItemFactory.createWeapon("player-primary-weapon", 10, Melee)

  val enemies: Set[Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.map {
    case (point, index) if index % 3 == 0 =>
      Entity(
        id = s"Rat $index",
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Enemy),
        Health(25),
        Initiative(12),
        Inventory(Nil, Some(s"rat-weapon-$index")),
        Drawable(Sprites.ratSprite),
        Hitbox(),
        DeathEvents(deathDetails => deathDetails.killerId.map {
          killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 4)
        }.toSeq
        )
      )
    case (point, index) if index % 3 == 1 =>
      Entity(
        id = s"Snake $index",
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Enemy),
        Health(18),
        Initiative(25),
        Inventory(Nil, Some(s"snake-weapon-$index")),
        Drawable(Sprites.snakeSprite),
        Hitbox(),
        DeathEvents(deathDetails =>
          deathDetails.killerId.map {
            killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 4)
          }.toSeq
        )
      )
    case (point, index) =>
      Entity(
        id = s"Slime $index",
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Enemy),
        Health(20),
        Initiative(15),
        Inventory(Nil, Some(s"slime-weapon-$index")),
        Drawable(Sprites.slimeSprite),
        Hitbox(),
        DeathEvents(deathDetails => {
          val experienceEvent = deathDetails.killerId.map {
            killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 4)
          }.toSeq
          experienceEvent ++ createSlimelets(deathDetails)
        })
      )
      
  }

  val player: Entity = dungeon.startPoint match {
    case point =>
      Entity(
        id = "Player ID",
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Player),
        Health(100),
        Initiative(10),
        Inventory(
          itemEntityIds = playerStartingItems.map(_.id).toSeq,
          primaryWeaponId = Some("player-primary-weapon"),
          secondaryWeaponId = None
        ),
        Equipment(),
        SightMemory(),
        Drawable(Sprites.playerSprite),
        Hitbox(),
        Experience(),
        DeathEvents()
      )
  }

  val items: Set[Entity] = dungeon.items.zipWithIndex.map {
    case ((point, itemDescriptor), index) =>
      val basePosition = Point(
        point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        point.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      
      // Create entity from descriptor and place in world
      val itemEntity = itemDescriptor.createEntity(s"item-$index")
      val placedEntity = itemEntity.addComponent(Movement(position = basePosition))
      
      // Add EntityTypeComponent for keys
      itemDescriptor match {
        case ItemDescriptor.KeyDescriptor(keyColour) =>
          placedEntity.addComponent(EntityTypeComponent(EntityType.Key(keyColour)))
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
    entities = Vector(player) ++ playerStartingItems ++ items ++ enemies ++ lockedDoors ++ 
               ratWeapons.values ++ snakeWeapons.values ++ slimeWeapons.values ++ Seq(playerPrimaryWeapon),
    dungeon = dungeon
  )
}
