package map

import data.Items.ItemReference
import game.entity.KeyColour.*
import game.entity.{KeyColour}
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
        val newRoomConnections = currentDungeon.roomConnections - roomConnection + roomConnection.copy(optLock = Some(LockedDoor(Red)))

        currentDungeon
          .addRoom(originRoom, direction1)
          .addRoom(keyRoom1, direction2)
          .lockRoomConnection(roomConnection, LockedDoor(Red))
          .addItem(keyRoom2, ItemReference.RedKey)
          .blockRoom(keyRoom2)
      }
    }.toSet
  }
}

class TreasureRoomMutator(targetTreasureRoomCount: Int, targetRoomCount: Int) extends DungeonMutator {
  val possibleItems: Set[ItemReference] = Set(
    ItemReference.HealingPotion, 
    ItemReference.FireballScroll, 
    ItemReference.Arrow,
    ItemReference.LeatherHelmet,
    ItemReference.ChainmailArmor,
    ItemReference.IronHelmet,
    ItemReference.PlateArmor,
    ItemReference.LeatherBoots,
    ItemReference.IronBoots,
    ItemReference.LeatherGloves,
    ItemReference.IronGloves,
    ItemReference.BasicSword,
    ItemReference.IronSword
  )

  override def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon] = {
    if (currentDungeon.nonKeyItems.size >= targetTreasureRoomCount || currentDungeon.roomGrid.size + 1 >= targetRoomCount) {
      Set.empty
    } else {
      // Get items already placed in the dungeon to avoid duplicates
      val placedItems = currentDungeon.nonKeyItems.map(_._2).toSet
      val availableItems = possibleItems -- placedItems
      
      for {
        (originRoom, direction) <- currentDungeon.availableRooms
        item <- availableItems
        if !currentDungeon.endpoint.contains(originRoom)
        treasureRoom = originRoom + direction
      } yield currentDungeon
        .addRoom(originRoom, direction)
        .blockRoom(treasureRoom)
        .addItem(treasureRoom, item)
    }
  }
}

class BossRoomMutator(targetRoomCount: Int) extends DungeonMutator {
  override def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon] = {
    // Only create boss room once the dungeon is at target size and has an endpoint
    if (currentDungeon.roomGrid.size == targetRoomCount && currentDungeon.endpoint.isDefined) {
      // Boss room is simply the endpoint room - no changes needed to dungeon structure
      // The boss will be placed in the endpoint room during enemy generation
      Set(currentDungeon.copy(hasBossRoom = true))
    } else {
      Set.empty
    }
  }
}

class TraderRoomMutator(targetRoomCount: Int) extends DungeonMutator {
  override def getPossibleMutations(currentDungeon: Dungeon): Set[Dungeon] = {
    // Only create trader room once, and only if we haven't already
    if (currentDungeon.traderRoom.isDefined) {
      // Already have a trader room, no more mutations needed
      Set.empty
    } else if (currentDungeon.roomGrid.size >= 2 && currentDungeon.roomGrid.size <= targetRoomCount - 1) {
      // Try to create a dedicated trader room adjacent to start point
      val adjacentRooms = currentDungeon.availableRooms(currentDungeon.startPoint)
      if (adjacentRooms.nonEmpty) {
        for {
          (originRoom, direction) <- adjacentRooms
          traderRoom = originRoom + direction
        } yield currentDungeon
          .addRoom(originRoom, direction)
          .blockRoom(traderRoom)
          .copy(traderRoom = Some(traderRoom))
      } else {
        // No available adjacent rooms, use an existing adjacent room or start point
        val fallbackRoom = game.Direction.values
          .map(dir => currentDungeon.startPoint + dir)
          .find(point => currentDungeon.roomGrid.contains(point))
          .getOrElse(currentDungeon.startPoint)
        
        Set(currentDungeon.copy(traderRoom = Some(fallbackRoom)))
      }
    } else if (currentDungeon.roomGrid.size > targetRoomCount - 1) {
      // We're at or near target size and still no trader room - just pick an existing room
      val fallbackRoom = game.Direction.values
        .map(dir => currentDungeon.startPoint + dir)
        .find(point => currentDungeon.roomGrid.contains(point))
        .getOrElse(currentDungeon.startPoint)
      
      Set(currentDungeon.copy(traderRoom = Some(fallbackRoom)))
    } else {
      // Not ready yet (size < 2)
      Set.empty
    }
  }
}
