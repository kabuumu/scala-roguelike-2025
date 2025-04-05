package dungeongenerator.pathfinder.nodefinders.room

import dungeongenerator.generator.Dungeon
import dungeongenerator.generator.Entity.*
import dungeongenerator.pathfinder.DungeonCrawlerAction._
import dungeongenerator.pathfinder.{DungeonCrawler, Node}
import dungeongenerator.pathfinder.nodefinders.NodeFinder

object AdjacentRoomNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, currentInventory, _), currentDungeon@Dungeon(dungeonEntities)) = currentNode

    for {
      (currentRoomLocation, currentRoom) <- dungeonEntities.collectFirst {
        case (roomPoint, room: Room) if roomPoint == currentPoint => roomPoint -> room
      }.toIterable

      (currentRoomDoorLocation, currentRoomDoor) <- dungeonEntities.collect {
        case (location, door: Door) if currentRoom.entities.exists(_._1 == location) => location -> door
      }
      (adjacentRoomLocation, _) <- dungeonEntities.collect {
        case (adjacentRoomLocation, adjacentRoom@Room(adjacentRoomEntities)) if adjacentRoomEntities.toSet.exists(_._1 == currentRoomDoorLocation)
          && adjacentRoomLocation != currentRoomLocation =>
          (adjacentRoomLocation, adjacentRoom)
      }
      newNode <-
        if (currentRoomDoor.optLock.contains(KeyLock) && currentInventory.contains(Key)) {
          Some(
            Node(
              DungeonCrawler(adjacentRoomLocation, currentInventory.diff(Seq(Key)), UnlockedDoor),
              currentDungeon.copy(
                entities = (dungeonEntities
                  - (currentRoomDoorLocation -> currentRoomDoor)
                  + (currentRoomDoorLocation -> Door(None))
                  )
              )
            ))
        } else if (currentRoomDoor.optLock.contains(BossKeyLock) && currentInventory.contains(BossKey)) {
        Some(
          Node(
            DungeonCrawler(adjacentRoomLocation, currentInventory.diff(Seq(BossKey)), UnlockedDoor),
            currentDungeon.copy(
              entities = (dungeonEntities
                - (currentRoomDoorLocation -> currentRoomDoor)
                + (currentRoomDoorLocation -> Door(None))
                )
            )
          ))
      } else if (currentRoomDoor.isOpen) {
          Some(
            Node(
              DungeonCrawler(adjacentRoomLocation, currentInventory, Moved),
              currentDungeon
            )
          )
        } else None
    } yield newNode
  }
}
