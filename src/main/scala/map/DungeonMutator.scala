package map

import dungeongenerator.generator.Entity.KeyColour
import dungeongenerator.generator.Entity.KeyColour.Red
import game.EntityType.LockedDoor
import game.Item
import game.Item.Key

trait DungeonMutator {
  def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon]
}

class EndPointMutator(distanceToTargetRoom: Int) extends DungeonMutator {
  override def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon] =
    currentDungeon.endpoint match {
      case None => for {
        (originRoom, direction) <- currentDungeon.availableRooms
      } yield currentDungeon.addRoom(originRoom, direction).copy(endpoint = Some(originRoom + direction))
      case Some(endpoint) if currentDungeon.dungeonPath.size < distanceToTargetRoom => for {
        (originRoom, direction) <- currentDungeon.availableRooms(endpoint)
      } yield currentDungeon.addRoom(originRoom, direction).copy(endpoint = Some(originRoom + direction))
      case _ =>
        Set.empty
    }
}

//TODO - Update this to have variable distance between key and lock
class KeyLockMutator(lockedDoorCount: Int) extends DungeonMutator {
  private val minRoomsPerLockedDoor: Int = 2

  override def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon] = {
    if (currentDungeon.lockedDoorCount >= lockedDoorCount || currentDungeon.roomGrid.size < minRoomsPerLockedDoor) {
      Set.empty
    } else {
      for {
        roomConnection@RoomConnection(originRoom, direction, destinationRoom, optLock) <- currentDungeon.dungeonPath
        if originRoom != currentDungeon.startPoint
        if optLock.isEmpty
        (originRoom, direction1) <- currentDungeon.availableRooms(originRoom)
        keyRoom1 = originRoom + direction1
        updatedDungeon = currentDungeon.addRoom(originRoom, direction1)
        (_, direction2) <- updatedDungeon.availableRooms(keyRoom1)
        keyRoom2 = keyRoom1 + direction2
      } yield {
        val newRoomConnections = currentDungeon.roomConnections - roomConnection + roomConnection.copy(optLock = Some(LockedDoor(KeyColour.Red)))

        currentDungeon
          .addRoom(originRoom, direction1)
          .addRoom(keyRoom1, direction2)
          .lockRoomConnection(roomConnection, LockedDoor(Red))
          .addItem(keyRoom2, Key(Red))
          .blockRoom(keyRoom2)
      }
    }.toSet
  }
}

class TreasureRoomMutator(targetTreasureRoomCount: Int, dungeonPathSize: Int) extends DungeonMutator {
  override def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon] = {
    if (currentDungeon.items.count(_._2 == Item.Potion) >= targetTreasureRoomCount || dungeonPathSize != currentDungeon.dungeonPath.size) {
      Set.empty
    } else {
      for {
        (originRoom, direction) <- currentDungeon.availableRooms
        if !currentDungeon.endpoint.contains(originRoom)
        treasureRoom = originRoom + direction
      } yield currentDungeon
        .addRoom(originRoom, direction)
        .blockRoom(treasureRoom)
        .addItem(treasureRoom, Item.Potion)
    }
  }
}
