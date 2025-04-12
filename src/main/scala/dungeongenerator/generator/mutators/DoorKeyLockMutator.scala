package dungeongenerator.generator.mutators

import dungeongenerator.generator.Entity.*
import dungeongenerator.generator.{Dungeon, DungeonGeneratorConfig}

case object DoorKeyLockMutator extends DungeonMutator {
  override def getPossibleMutations(dungeon: Dungeon, config: DungeonGeneratorConfig): Iterable[Dungeon] =
    for {
      doorToLock <- dungeon.entities.filter(_._2 == Door(optLock = None))
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
    }
}
