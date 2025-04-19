package game

import game.Item.{Item, Key, Potion}
import map.{Dungeon, MapGenerator}

object StartingState {
  val dungeon: Dungeon = MapGenerator.generateDungeon(12, 3)

  val enemies: Set[Entity] = (dungeon.roomGrid - dungeon.startPoint).map {
    point =>
      Entity(
        xPosition = point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        yPosition = point.y * Dungeon.roomSize + Dungeon.roomSize / 2,
        entityType = EntityType.Enemy,
        health = Health(1),
      )
  }

  val player: Entity = dungeon.startPoint match {
    case point =>
      Entity(
        id = "Player ID",
        xPosition = point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        yPosition = point.y * Dungeon.roomSize + Dungeon.roomSize / 2,
        entityType = EntityType.Player,
        health = Health(12),
        inventory = Seq(Potion, Potion)
      )
  }

  val items: Set[Entity] = dungeon.items.collect {
    case (point, Item.Key(keyColour)) =>
      Entity(
        xPosition = point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        yPosition = point.y * Dungeon.roomSize + Dungeon.roomSize / 2,
        entityType = EntityType.Key(keyColour),
        health = Health(0)
      )
    case (point, Item.Potion) =>
      Entity(
        xPosition = point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        yPosition = point.y * Dungeon.roomSize + Dungeon.roomSize / 2,
        entityType = EntityType.ItemEntity(Item.Potion),
        health = Health(0)
      )
  }

  val lockedDoors: Set[Entity] = dungeon.lockedDoors.map {
    case (point, lockedDoor) =>
      Entity(
        xPosition = point.x,
        yPosition = point.y,
        entityType = lockedDoor,
        health = Health(0)
      )
  }

  val startingGameState: GameState = GameState(
    playerEntityId = player.id,
    entities = Vector(player) ++ enemies ++ items ++ lockedDoors,
    dungeon = dungeon
  )
}
