package game

import game.Item.Potion
import map.{Dungeon, MapGenerator, TileType}

object StartingState {
  val dungeon: Dungeon = MapGenerator.generateDungeon(10)
  val startPoint: Point = dungeon.roomGrid.head

  val mapTiles: Set[Entity] = dungeon.tiles.collect {
    case (point, TileType.Wall) =>
      Entity(xPosition = point.x, yPosition = point.y, entityType = EntityType.Wall, health = Health(0), lineOfSightBlocking = true)
    case (point, TileType.Floor) =>
      Entity(xPosition = point.x, yPosition = point.y, entityType = EntityType.Floor, health = Health(0), lineOfSightBlocking = false)
  }.toSet

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

  val startingGameState: GameState = GameState(player.id, Vector(player) ++ mapTiles ++ enemies)
}
