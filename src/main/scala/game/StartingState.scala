package game

import data.Sprites
import game.Item.*
import game.entity.*
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
        Initiative(10),
        Controller(),
        Sprites.ratSprite,
      )
    case (point, _) =>
      Entity(
        Movement(position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.Enemy),
        Health(1),
        Initiative(20),
        Inventory(Nil, Some(Weapon(1, Ranged(4)))),
        Controller(),
        Sprites.snakeSprite,
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
        Initiative(10),
        Inventory(
          items = Seq(Potion),
          primaryWeapon = Some(Weapon(2, Melee)),
          secondaryWeapon = Some(Weapon(1, Ranged(6)))
        ),
        SightMemory(),
        Controller(),
        Sprites.playerSprite,
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
        keyColour match {
          case KeyColour.Yellow => Sprites.yellowKeySprite
          case KeyColour.Blue => Sprites.blueKeySprite
          case KeyColour.Red => Sprites.redKeySprite
        }
      )
    case (point, Item.Potion) =>
      Entity(
        Movement(Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        )),
        EntityTypeComponent(EntityType.ItemEntity(Item.Potion)),
        Sprites.potionSprite
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
        lockedDoor.keyColour match {
          case KeyColour.Yellow => Sprites.yellowDoorSprite
          case KeyColour.Blue => Sprites.blueDoorSprite
          case KeyColour.Red => Sprites.redDoorSprite
        }
      )
  }

  val startingGameState: GameState = GameState(
    playerEntityId = player.id,
    entities = Vector(player) ++ items ++ enemies ++ lockedDoors,
    dungeon = dungeon
  )
}
