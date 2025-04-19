package map

import dungeongenerator.generator.Entity.KeyColour
import game.EntityType.LockedDoor
import game.Item

trait DungeonMutator {
  def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon]
}

class NewRoomMutator(dungeonSize: Int) extends DungeonMutator {
  override def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon] = {
    if (currentDungeon.roomGrid.size >= dungeonSize) {
      Set.empty
    } else for {
      (originRoom, direction) <- currentDungeon.availableRooms
    } yield currentDungeon.addRoom(originRoom, direction)
  }
}

class EndPointMutator(distanceToTargetRoom: Int) extends DungeonMutator {
  override def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon] = {
    if (currentDungeon.endpoint.isDefined) {
      Set.empty
    } else {
      for {
        targetRoom <- currentDungeon.roomGrid
        if targetRoom != currentDungeon.startPoint
        if currentDungeon.roomConnections.count(_.originRoom == targetRoom) == 1
        path = RoomGridPathfinder.findPath(
          rooms = currentDungeon.roomGrid,
          roomConnections = currentDungeon.roomConnections,
          startPoint = currentDungeon.startPoint,
          target = targetRoom
        )
        if path.size >= distanceToTargetRoom
      } yield currentDungeon.copy(endpoint = Some(targetRoom))
    }
  }
}


class KeyLockMutator(lockedDoorCount: Int) extends DungeonMutator {
  private val minRoomsPerLockedDoor: Int = 4

  override def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon] = {
    if (currentDungeon.lockedDoorCount >= lockedDoorCount || currentDungeon.endpoint.isEmpty || currentDungeon.roomGrid.size < minRoomsPerLockedDoor) {
      Set.empty
    } else {
      for {
        roomConnection@RoomConnection(originRoom, direction, destinationRoom, optLock) <- currentDungeon.dungeonPath
        if optLock.isEmpty
        keyRoom <- currentDungeon.roomGrid
        newRoomConnections = currentDungeon.roomConnections - roomConnection + roomConnection.copy(optLock = Some(LockedDoor(KeyColour.Red)))
        keyRoomPath = RoomGridPathfinder.findPath(
          rooms = currentDungeon.roomGrid,
          roomConnections = newRoomConnections,
          startPoint = originRoom,
          target = keyRoom
        )
        startToKeyRoomPath = RoomGridPathfinder.findPath(
          rooms = currentDungeon.roomGrid,
          roomConnections = newRoomConnections,
          startPoint = currentDungeon.startPoint,
          target = keyRoom
        )
        if keyRoom != originRoom && keyRoom != currentDungeon.startPoint
        if currentDungeon.roomConnections(keyRoom).size == 1
        if !keyRoomPath.exists(connection => connection.originRoom == destinationRoom || connection.isLocked)
        if startToKeyRoomPath.size > keyRoomPath.size
        if keyRoomPath.size >= 3
      } yield {

        currentDungeon.copy(
          roomConnections = newRoomConnections,
          items = currentDungeon.items + (keyRoom -> Item.Key(KeyColour.Red)),
          blockedRooms = currentDungeon.blockedRooms + keyRoom,
        )
      }
    }.toSet
  }
}
