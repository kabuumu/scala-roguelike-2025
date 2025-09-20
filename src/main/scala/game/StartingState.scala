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

  // Create snake spit abilities for snakes
  val snakeSpitAbilities: Map[Int, Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.collect {
    case (_, index) if index % 3 == 1 => index -> Items.snakeSpit(s"Snake-$index-spit")
  }.toMap

  val enemies: Set[Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.map {
    case (point, index) if index % 3 == 0 =>
      val position = Point(
        point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        point.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      Enemies.rat(s"Rat $index", position)
    case (point, index) if index % 3 == 1 =>
      val position = Point(
        point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        point.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      Enemies.snake(s"Snake $index", position, s"Snake-$index-spit")
    case (point, index) =>
      val position = Point(
        point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        point.y * Dungeon.roomSize + Dungeon.roomSize / 2
      )
      Enemies.slime(s"Slime $index", position)
      
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
          itemEntityIds = (playerStartingItems ++ playerStartingEquipment).map(_.id).toSeq
        ),
        Equipment(
          armor = Some(Equippable.armor(EquipmentSlot.Armor, 5, "Chainmail Armor")),
          weapon = Some(Equippable.weapon(3, "Basic Sword"))
        ),
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
    entities = Vector(player) ++ playerStartingItems ++ playerStartingEquipment ++ items ++ enemies ++ lockedDoors ++ snakeSpitAbilities.values,
    dungeon = dungeon
  )
}
