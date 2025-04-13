package dungeongenerator.generator.mutators

import dungeongenerator.generator.Entity.TreasureRoom
import dungeongenerator.generator.{Dungeon, DungeonGeneratorConfig}

case object TreasureRoomMutator extends DungeonMutator {
  override def getPossibleMutations(dungeon: Dungeon, config: DungeonGeneratorConfig): Iterable[Dungeon] = {
    for {
      treasureRoom <- CreateRoomMutator.createAdditionalRooms(dungeon, maxRoomSize = 8)
    } yield
      dungeon + treasureRoom + (treasureRoom.center -> TreasureRoom)
  }
}
