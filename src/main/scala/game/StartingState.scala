package game

import dungeongenerator.generator
import dungeongenerator.generator.Entity.*
import dungeongenerator.generator.{DefaultDungeonGeneratorConfig, DungeonGenerator}
import game.Item.Potion

object StartingState {
  val dungeon: generator.Dungeon = {
    val startTime = System.currentTimeMillis()
    val dungeon = DungeonGenerator.generatePossibleDungeons(config = DefaultDungeonGeneratorConfig).head
    val endTime = System.currentTimeMillis()
    println(s"Dungeon generation took ${endTime - startTime} milliseconds")
    println(s"Generated ${dungeon.roomCount} rooms with a longest path of ${dungeon.longestRoomPath.size}")
    println(s"Dungeon has ${dungeon.nonPathRooms.size} non-path rooms")

    dungeon
  }

  val mapTiles: Set[Entity] = dungeon.entities.collect {
    case (generator.Point(x, y), generator.Entity.Wall) =>
      Entity(xPosition = x, yPosition = y, entityType = EntityType.Wall, health = Health(0), lineOfSightBlocking = true)
    case (generator.Point(x, y), generator.Entity.Floor | generator.Entity.Door(None)) =>
      Entity(xPosition = x, yPosition = y, entityType = EntityType.Floor, health = Health(0), lineOfSightBlocking = false)
    case (generator.Point(x, y), generator.Entity.Door(Some(ItemLock(Key(keyColour))))) =>
      Entity(xPosition = x, yPosition = y, entityType = EntityType.LockedDoor(keyColour), health = Health(0), lineOfSightBlocking = true)
    case (generator.Point(x, y), generator.Entity.Key(keyColour)) =>
      Entity(xPosition = x, yPosition = y, entityType = EntityType.Key(keyColour), health = Health(0), lineOfSightBlocking = false)
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

  val items: Set[Entity] = dungeon.nonPathRooms.map{
    point =>
      Entity(
        xPosition = point.x,
        yPosition = point.y,
        entityType = EntityType.ItemEntity(Potion),
        health = Health(0),
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

  val startingGameState: GameState = GameState(player.id, Set(player) ++ mapTiles ++ enemies ++ items)
}
