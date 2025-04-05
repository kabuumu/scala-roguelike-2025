package dungeongenerator.generator.mutators

import dungeongenerator.generator.Entity._
import dungeongenerator.generator.{Dungeon, DungeonGeneratorConfig}

object CreateBossRoomMutator extends DungeonMutator {
  override def getPossibleMutations(dungeon: Dungeon, config: DungeonGeneratorConfig): Iterable[Dungeon] =
    if (dungeon.roomCount >= config.preBossRoomCount && dungeon.count[BossRoom.type] == 0) for {
      bossRoom <- CreateRoomMutator.createAdditionalRooms(dungeon, minRoomSize = 16)
      bossKeyRoom <- CreateRoomMutator.createAdditionalRooms(dungeon, maxRoomSize = 8)
    } yield {
      dungeon.copy(
        entities = dungeon.entities
          .++(bossRoom.entities)
          .+(bossRoom.center -> BossRoom)
          .filterNot { case (point, _) =>
            point == bossRoom.intersectPoint // Remove the wall where the rooms intersect
          }.+(bossRoom.intersectPoint -> Door(optLock = Some(BossKeyLock)))
          .++(bossKeyRoom.entities)
          .+(bossKeyRoom.center -> BossKey)
          .filterNot { case (point, _) => point == bossKeyRoom.intersectPoint}
          .+(bossKeyRoom.intersectPoint -> Door(optLock = None)) //Add a door where the rooms intersect
      )
    } else Nil
}
