package game

import data.Sprites
import game.EnemyAI.DefaultAI
import game.Item.*
import game.entity.*
import game.entity.UpdateAction.{AIAction, UpdateInitiative}
import map.{Dungeon, MapGenerator}

object StartingState {
  val dungeon: Dungeon = MapGenerator.generateDungeon(dungeonSize = 20, lockedDoorCount = 4)

  val enemies: Set[Entity] = (dungeon.roomGrid - dungeon.startPoint).zipWithIndex.map {
    case (point, index) if index % 2 == 0 =>
      Entity(
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Enemy),
        Health(2),
        Initiative(40),
        UpdateController(UpdateInitiative, AIAction(DefaultAI)),
        Drawable(Sprites.ratSprite),
        Hitbox()
      )
    case (point, _) =>
      Entity(
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Enemy),
        Health(1),
        Initiative(80),
        Inventory(Nil, Some(Weapon(1, Ranged(4)))),
        UpdateController(UpdateInitiative, AIAction(DefaultAI)),
        Drawable(Sprites.snakeSprite),
        Hitbox()
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
        Health(12),
        Initiative(35),
        Inventory(
          items = Seq(Potion, Scroll, Bow) ++ Seq.fill(6)(Arrow),
          primaryWeapon = Some(Weapon(2, Melee)),
          secondaryWeapon = None
        ),
        SightMemory(),
        UpdateController(UpdateInitiative),
        Drawable(Sprites.playerSprite),
        Hitbox(),
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
