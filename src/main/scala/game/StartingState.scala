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
  
  // Create weapons as entities for enemies and player
  val ratWeapons: Map[Int, Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.collect {
    case (_, index) if index % 3 == 0 => index -> Items.weapon(s"rat-weapon-$index", 8, Melee)
  }.toMap
  
  val snakeWeapons: Map[Int, Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.collect {
    case (_, index) if index % 3 == 1 => index -> Items.weapon(s"snake-weapon-$index", 6, Ranged(4))
  }.toMap
  
  val slimeWeapons: Map[Int, Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.collect {
    case (_, index) if index % 3 == 2 => index -> Items.weapon(s"slime-weapon-$index", 6, Melee)
  }.toMap
  
  val playerPrimaryWeapon: Entity = Items.weapon("player-primary-weapon", 10, Melee)

  val enemies: Set[Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.map {
    case (point, index) if index % 3 == 0 =>
      val position = Point(
        point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        point.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      Enemies.rat(s"Rat $index", position, Some(s"rat-weapon-$index"))
    case (point, index) if index % 3 == 1 =>
      val position = Point(
        point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        point.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      Enemies.snake(s"Snake $index", position, Some(s"snake-weapon-$index"))
    case (point, index) =>
      val position = Point(
        point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        point.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      Enemies.slime(s"Slime $index", position, Some(s"slime-weapon-$index"))
      
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
        EventMemory(),
        Drawable(Sprites.playerSprite),
        Hitbox(),
        Experience(),
        DeathEvents()
      )
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
    entities = Vector(player) ++ playerStartingItems ++ items ++ enemies ++ lockedDoors ++ 
               ratWeapons.values ++ snakeWeapons.values ++ slimeWeapons.values ++ Seq(playerPrimaryWeapon),
    dungeon = dungeon
  )
}
