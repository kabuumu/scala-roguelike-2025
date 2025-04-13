package dungeongenerator.pathfinder.nodefinders.room

import dungeongenerator.generator.Dungeon
import dungeongenerator.generator.Entity.*
import dungeongenerator.pathfinder.DungeonCrawlerAction.*
import dungeongenerator.pathfinder.nodefinders.NodeFinder
import dungeongenerator.pathfinder.{DungeonCrawler, Node}

object AdjacentRoomNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, currentInventory, _), currentDungeon@Dungeon(dungeonEntities)) = currentNode

    for {
      (currentRoomLocation, currentRoom) <- dungeonEntities.collectFirst {
        case (roomPoint, room: Room) if roomPoint == currentPoint => roomPoint -> room
      }.toSeq

      (currentRoomDoorLocation, currentRoomDoor) <- dungeonEntities.collect {
        case (location, door: Door) if currentRoom.entities.exists(_._1 == location) => location -> door
      }
      (adjacentRoomLocation, adjacentRoom) <- dungeonEntities.collect {
        case (adjacentRoomLocation, adjacentRoom@Room(adjacentRoomEntities)) if adjacentRoomEntities.toSet.exists(_._1 == currentRoomDoorLocation)
          && adjacentRoomLocation != currentRoomLocation =>
          (adjacentRoomLocation, adjacentRoom)
      }
      newNode <- currentRoomDoor match {
        case lockedDoor @ Door(Some(ItemLock(keyItem))) if currentInventory.contains(keyItem) =>
          Some(currentNode.updateCrawler(
              _.removeItem(keyItem)
                .addAction(UnlockedDoor)
            )
            .updateDungeon(
              _.copy(
                entities = dungeonEntities
                  - (currentRoomDoorLocation -> lockedDoor)
                  + (currentRoomDoorLocation -> Door(None))
              )
            )
          )
        case Door(Some(BossKeyLock)) if currentInventory.contains(BossKey) =>
          Some(currentNode.updateCrawler(
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
          )
        case Door(None) =>
          Some(
            currentNode.updateCrawler(
              _.setLocation(adjacentRoomLocation)
                .addAction(Moved(currentRoomLocation, currentRoomDoorLocation, adjacentRoomLocation))
            )
          )
        case _ =>
          None
      }
    } yield newNode
  }
}
