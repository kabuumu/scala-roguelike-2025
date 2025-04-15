package game

import game.Item.Potion
import map.{Dungeon, MapGenerator, TileType}

object StartingState {
  val dungeon: Dungeon = MapGenerator.generateDungeon(20)
  val startPoint: Point = dungeon.roomGrid.head


  val enemies: Set[Entity] = (dungeon.roomGrid - startPoint).map {
    point =>
      Entity(
        xPosition = point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        yPosition = point.y * Dungeon.roomSize + Dungeon.roomSize / 2,
        entityType = EntityType.Enemy,
        health = Health(2)
      )
  }

  val player: Entity = startPoint match {
    case point =>
      Entity(
        id = "Player ID",
        xPosition = point.x * Dungeon.roomSize + Dungeon.roomSize / 2,
        yPosition = point.y * Dungeon.roomSize + Dungeon.roomSize / 2,
        entityType = EntityType.Player,
        health = Health(10),
        inventory = Seq(Potion)
      )
  }

  val startingGameState: GameState = GameState(
    playerEntityId = player.id,
    entities = Vector(player) ++ enemies,
    dungeon = dungeon
  )
}
