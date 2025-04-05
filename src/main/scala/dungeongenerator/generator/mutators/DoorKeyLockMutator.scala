package dungeongenerator.generator.mutators

import dungeongenerator.generator.Entity._
import dungeongenerator.generator.{Dungeon, DungeonGeneratorConfig}

case object DoorKeyLockMutator extends DungeonMutator {
  override def getPossibleMutations(dungeon: Dungeon, config: DungeonGeneratorConfig): Iterable[Dungeon] =
    if (dungeon.roomCount > config.preDoorRoomCount && dungeon.roomCount > (dungeon.lockedDoorCount + 1) * 2) for {
      doorToLock <- dungeon.entities.filter(_._2 == Door(optLock = None))

      path = dungeon.longestRoomPath

      //    lockedDoorRoomLocations: Option[Point] = doorToLock match {
      //      case (lockedDoorLocation, _) => path.last.dungeonState.entities.collectFirst {
      //        case (roomPoint, room: Room) if room.entities.exists(_._1 == lockedDoorLocation) =>
      //          roomPoint
      //      }
      //    }
      //    seenLockedDoor = path.map(_.currentCrawler.location).toSet.exists(lockedDoorRoomLocations.contains)
      //    if seenLockedDoor


      keyRoom <- CreateRoomMutator.createAdditionalRooms(dungeon, maxRoomSize = 8)
    } yield {
      val lockedDoor = doorToLock.copy(_2 = Door(Some(KeyLock)))

      dungeon.copy(
        entities = (dungeon.entities
          ++ keyRoom.entities
          - doorToLock
          + lockedDoor
          + (keyRoom.center -> Key)
          ).filterNot { case (point, _) =>
          point == keyRoom.intersectPoint // Remove the wall where the rooms intersect
        } + (keyRoom.intersectPoint -> Door(optLock = None)) //Add a door where the rooms intersect
      )
    } else Nil
}
