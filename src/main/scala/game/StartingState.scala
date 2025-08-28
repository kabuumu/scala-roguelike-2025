package game

import data.Sprites
import game.Item.*
import game.entity.*
import game.entity.ItemWithId
import game.entity.Experience.experienceForLevel
import game.system.event.GameSystemEvent.AddExperienceEvent
import map.{Dungeon, MapGenerator}


object StartingState {
  val dungeon: Dungeon = MapGenerator.generateDungeon(dungeonSize = 20, lockedDoorCount = 3, itemCount = 6)

  val enemies: Set[Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.map {
    case (point, index) if index % 2 == 0 =>
      Entity(
        id = s"Rat $index",
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Enemy),
        Health(25),
        Initiative(12),
        Inventory(Nil, Some(Weapon(8, Melee))),
        Drawable(Sprites.ratSprite),
        Hitbox(),
        DeathEvents(deathDetails => deathDetails.killerId.map {
          killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 4)
        }.toSeq
        )
      )
    case (point, index) =>
      Entity(
        id = s"Snake $index",
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Enemy),
        Health(18),
        Initiative(25),
        Inventory(Nil, Some(Weapon(6, Ranged(4)))),
        Drawable(Sprites.snakeSprite),
        Hitbox(),
        DeathEvents(deathDetails =>
          deathDetails.killerId.map {
            killerId => AddExperienceEvent(killerId, experienceForLevel(2) / 4)
          }.toSeq
        )
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
          items = (Seq(Potion, Scroll, Bow) ++ Seq.fill(6)(Arrow)).map(ItemWithId(_)),
          primaryWeapon = Some(Weapon(10, Melee)),
          secondaryWeapon = None
        ),
        Equipment(),
        SightMemory(),
        Drawable(Sprites.playerSprite),
        Hitbox(),
        Experience(),
        DeathEvents()
      )
  }

  val items: Set[Entity] = dungeon.items.collect {
    case (point, Item.Key(keyColour)) =>
      Entity(
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Key(keyColour)),
        Hitbox(),
        keyColour match {
          case KeyColour.Yellow => Drawable(Sprites.yellowKeySprite)
          case KeyColour.Blue => Drawable(Sprites.blueKeySprite)
          case KeyColour.Red => Drawable(Sprites.redKeySprite)
        }
      )
    case (point, Item.Potion) =>
      Entity(
        Movement(Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.ItemEntity(Item.Potion)),
        Hitbox(),
        Drawable(Sprites.potionSprite)
      )
    case (point, Item.Scroll) =>
      Entity(
        Movement(Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.ItemEntity(Item.Scroll)),
        Hitbox(),
        Drawable(Sprites.scrollSprite)
      )
    case (point, Item.Arrow) =>
      Entity(
        Movement(Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.ItemEntity(Item.Arrow)),
        Hitbox(),
        Drawable(Sprites.arrowSprite)
      )
    case (point, item: Item.EquippableItem) =>
      val sprite = item match {
        case Item.LeatherHelmet => Sprites.leatherHelmetSprite
        case Item.IronHelmet => Sprites.ironHelmetSprite
        case Item.ChainmailArmor => Sprites.chainmailArmorSprite
        case Item.PlateArmor => Sprites.plateArmorSprite
      }
      Entity(
        Movement(Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.ItemEntity(item)),
        Hitbox(),
        Drawable(sprite)
      )
  }

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
    entities = Vector(player) ++ items ++ enemies ++ lockedDoors,
    dungeon = dungeon
  )
}
