package map

import game.Item
import game.Item.KeyColour.*
import game.Item.{Item, Key, KeyColour}
import game.entity.EntityType.LockedDoor

trait DungeonMutator {
  def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon]
}

class EndPointMutator(targetRoomCount: Int) extends DungeonMutator {
  override def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon] =
    currentDungeon.endpoint match {
      case None => for {
        (originRoom, direction) <- currentDungeon.availableRooms
      } yield currentDungeon.addRoom(originRoom, direction).copy(endpoint = Some(originRoom + direction))
      case Some(endpoint) if currentDungeon.roomGrid.size < targetRoomCount => for {
        (originRoom, direction) <- currentDungeon.availableRooms(endpoint)
      } yield currentDungeon.addRoom(originRoom, direction).copy(endpoint = Some(originRoom + direction))
      case _ =>
        Set.empty
    }
}

//TODO - Update this to have variable distance between key and lock
class KeyLockMutator(lockedDoorCount: Int, targetRoomCount: Int) extends DungeonMutator {
  private val minRoomsPerLockedDoor: Int = 2

  override def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon] = {
    if (currentDungeon.lockedDoorCount >= lockedDoorCount
      || currentDungeon.roomGrid.size < minRoomsPerLockedDoor
      || currentDungeon.roomGrid.size + 2 > targetRoomCount
    ) {
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

class TreasureRoomMutator(targetTreasureRoomCount: Int, targetRoomCount: Int) extends DungeonMutator {
  val possibleItems: Set[Item] = Set(Item.Potion, Item.Scroll, Item.Arrow)

  override def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon] = {
    if (currentDungeon.nonKeyItems.size >= targetTreasureRoomCount || currentDungeon.roomGrid.size + 1 >= targetRoomCount) {
      Set.empty
    } else {
      for {
        (originRoom, direction) <- currentDungeon.availableRooms
        item <- possibleItems
        if !currentDungeon.endpoint.contains(originRoom)
        treasureRoom = originRoom + direction
      } yield currentDungeon
        .addRoom(originRoom, direction)
        .blockRoom(treasureRoom)
        .addItem(treasureRoom, item)
    }
  }
}
