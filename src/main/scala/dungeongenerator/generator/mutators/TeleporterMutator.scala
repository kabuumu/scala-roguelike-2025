package dungeongenerator.generator.mutators

import dungeongenerator.generator.Entity._
import dungeongenerator.generator.{Dungeon, DungeonGeneratorConfig}
import dungeongenerator.pathfinder.DungeonCrawlerAction.*

case object TeleporterMutator extends DungeonMutator {
  override def getPossibleMutations(dungeon: Dungeon, config: DungeonGeneratorConfig): Iterable[Dungeon] = for {
    teleporterRoomA <- CreateRoomMutator.createAdditionalRooms(dungeon, maxRoomSize = 8)
    dungeonWithTeleporterRoom = dungeon ++ teleporterRoomA.entities + (teleporterRoomA.intersectPoint -> Door(None))
    (teleporterTarget, _) <- dungeon.entities.filter(_._2.isInstanceOf[Room])
    doorToRemove@(doorLocation, _) <- dungeon.entities.filter(_._2 == Door(None))
    teleporterA = Teleporter(teleporterTarget)
  } yield (
    dungeonWithTeleporterRoom
      + (teleporterRoomA.center -> teleporterA)
      - doorToRemove
      + (doorLocation -> Wall)
    )
}
