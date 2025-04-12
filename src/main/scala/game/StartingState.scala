package game

import dungeongenerator.generator
import dungeongenerator.generator.Entity.*
import dungeongenerator.generator.{DefaultDungeonGeneratorConfig, DungeonGenerator}
import game.Item.Potion

object StartingState {
  val dungeon: generator.Dungeon = DungeonGenerator.generatePossibleDungeonsLinear(config = DefaultDungeonGeneratorConfig).head

  val mapTiles: Set[Entity] = dungeon.entities.collect {
    case (generator.Point(x, y), generator.Entity.Wall) =>
      Entity(xPosition = x, yPosition = y, entityType = EntityType.Wall, health = Health(0), lineOfSightBlocking = true)
    case (generator.Point(x, y), generator.Entity.Floor | generator.Entity.Door(None)) =>
      Entity(xPosition = x, yPosition = y, entityType = EntityType.Floor, health = Health(0), lineOfSightBlocking = false)
    case (generator.Point(x, y), generator.Entity.Door(Some(KeyLock))) =>
      Entity(xPosition = x, yPosition = y, entityType = EntityType.Door, health = Health(0), lineOfSightBlocking = true)
    case (generator.Point(x, y), generator.Entity.Key) =>
      Entity(xPosition = x, yPosition = y, entityType = EntityType.Key, health = Health(0), lineOfSightBlocking = false)
  }

  val enemies: Set[Entity] = (dungeon.roomLocations -- dungeon.optStartPoint).map {
    case point =>
      Entity(
        xPosition = point.x,
        yPosition = point.y,
        entityType = EntityType.Enemy,
        health = Health(2)
      )
  }

  val player: Entity = dungeon.optStartPoint match {
    case Some(point) =>
      Entity(
        id = "Player ID",
        xPosition = point.x,
        yPosition = point.y,
        entityType = EntityType.Player,
        health = Health(10),
        inventory = Seq(Potion)
      )
    case None =>
      throw new Exception("Player start point not found")
  }

  val startingGameState: GameState = GameState(player.id, Set(player) ++ mapTiles ++ enemies)
}
