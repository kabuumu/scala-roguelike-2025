package dungeongenerator.generator.mutators

import dungeongenerator.generator.Entity._
import dungeongenerator.generator.{Dungeon, DungeonGeneratorConfig}

case object DoorSwitchLockMutator extends DungeonMutator {
  override def getPossibleMutations(dungeon: Dungeon, config: DungeonGeneratorConfig): Iterable[Dungeon] =
    if (dungeon.roomCount > config.preDoorRoomCount && dungeon.roomCount > (dungeon.lockedDoorCount + 1) * 2) for {
      doorToLock <- dungeon.entities.filter(_._2 == Door(optLock = None))
      switchRoom <- CreateRoomMutator.createAdditionalRooms(dungeon, maxRoomSize = 8)
    } yield {
      val lockedDoor = doorToLock.copy(_2 = Door(Some(SwitchLock)))
      val switchFunction = (switchDungeon: Dungeon) =>
        switchDungeon.copy(
          entities = (switchDungeon.entities
            - lockedDoor
            + doorToLock
            )
        )

      dungeon.copy(
        entities = (dungeon.entities
          ++ switchRoom.entities
          - doorToLock
          + lockedDoor
          + (switchRoom.center -> Switch(switchFunction))
          ).filterNot { case (point, _) =>
          point == switchRoom.intersectPoint
        } + (switchRoom.intersectPoint -> Door(optLock = None))

      )
    } else Nil
}
