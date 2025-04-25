package game

import game.Item.{Item, Key, Potion, Weapon}
import map.{Dungeon, MapGenerator}

object StartingState {
  val dungeon: Dungeon = MapGenerator.generateDungeon(12, 3)

  val enemies: Set[Entity] = (dungeon.roomGrid - dungeon.startPoint).map {
    point =>
      Entity(
        position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        ),
        entityType = EntityType.Enemy,
        health = Health(2),
      )
  }

  val player: Entity = dungeon.startPoint match {
    case point =>
      Entity(
        id = "Player ID",
        position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        ),
        entityType = EntityType.Player,
        health = Health(12),
        inventory = Inventory(
          items = Seq(Potion),
          primaryWeapon = Some(Weapon(2, 1)),
          secondaryWeapon = Some(Weapon(1, 6))
        )
      )
  }

  val items: Set[Entity] = dungeon.items.collect {
    case (point, Item.Key(keyColour)) =>
      Entity(
        position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        ),
        entityType = EntityType.Key(keyColour),
        health = Health(0)
      )
    case (point, Item.Potion) =>
      Entity(
        position = Point(
          point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
          point.y * Dungeon.roomSize + Dungeon.roomSize / 2
        ),
        entityType = EntityType.ItemEntity(Item.Potion),
        health = Health(0)
      )
  }

  val lockedDoors: Set[Entity] = dungeon.lockedDoors.map {
    case (point, lockedDoor) =>
      Entity(
        position = Point(
          point.x,
          point.y
        ),
        entityType = lockedDoor,
        health = Health(0),
        lineOfSightBlocking = true,
      )
  }

  val startingGameState: GameState = GameState(
    playerEntityId = player.id,
    entities = Vector(player) ++ enemies ++ items ++ lockedDoors,
    dungeon = dungeon
  )
}
