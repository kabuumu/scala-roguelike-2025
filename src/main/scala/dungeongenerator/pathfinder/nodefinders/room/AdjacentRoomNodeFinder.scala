package dungeongenerator.pathfinder.nodefinders.room

import dungeongenerator.generator.Dungeon
import dungeongenerator.generator.Entity.*
import dungeongenerator.pathfinder.DungeonCrawlerAction.*
import dungeongenerator.pathfinder.nodefinders.NodeFinder
import dungeongenerator.pathfinder.{DungeonCrawler, Node}

object AdjacentRoomNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, currentInventory, _), currentDungeon@Dungeon(dungeonEntities)) = currentNode

    val adjacentRooms = for {
      (currentRoomLocation, currentRoom) <- dungeonEntities.collectFirst {
        case (roomPoint, room: Room) if roomPoint == currentPoint => roomPoint -> room
      }.toSeq

      (currentRoomDoorLocation, currentRoomDoor) <- dungeonEntities.collect {
        case (location, door: Door) if currentRoom.entities.exists(_._1 == location) => location -> door
      }
      (adjacentRoomLocation, _) <- dungeonEntities.collect {
        case (adjacentRoomLocation, adjacentRoom@Room(adjacentRoomEntities)) if adjacentRoomEntities.toSet.exists(_._1 == currentRoomDoorLocation)
          && adjacentRoomLocation != currentRoomLocation =>
          (adjacentRoomLocation, adjacentRoom)
      }
    } yield (currentRoomDoorLocation, currentRoomDoor, adjacentRoomLocation)

    adjacentRooms.collect {
      case (currentRoomDoorLocation, Door(Some(KeyLock)), adjacentRoomLocation) if currentInventory.contains(Key) =>
        currentNode.updateCrawler(
            _.removeItem(Key)
              .addAction(UnlockedDoor)
          )
          .updateDungeon(
            _.copy(
              entities = dungeonEntities
                - (currentRoomDoorLocation -> Door(Some(KeyLock)))
                + (currentRoomDoorLocation -> Door(None))
            )
          )
      case (currentRoomDoorLocation, Door(Some(BossKeyLock)), adjacentRoomLocation) if currentInventory.contains(BossKey) =>
        currentNode.updateCrawler(
            _.removeItem(BossKey)
              .addAction(UnlockedDoor)
          )
          .updateDungeon(
            _.copy(
              entities = dungeonEntities
                - (currentRoomDoorLocation -> Door(Some(BossKeyLock)))
                + (currentRoomDoorLocation -> Door(None))
            )
          )
      case (_, Door(None), adjacentRoomLocation) =>
        currentNode.updateCrawler(
            _.setLocation(adjacentRoomLocation)
              .addAction(Moved)
          )
    }
  }
}
