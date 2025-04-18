package dungeongenerator.generator

import dungeongenerator.generator.Entity.BossRoom
import dungeongenerator.generator.mutators.*
import dungeongenerator.generator.mutators.CreateBossRoomMutator.*
import dungeongenerator.generator.predicates.*
import dungeongenerator.pathfinder.nodefinders.NodeFinder
import dungeongenerator.pathfinder.nodefinders.room.*

case class DungeonGeneratorConfig(targetCount: Int,
                                  preDoorRoomCount: Int = 2,
                                  preBossRoomCount: Int = 2,
                                  mutators: Set[DungeonMutator],
                                  nodeFinders: Set[NodeFinder],
                                  predicates: Set[DungeonPredicate])

object DefaultDungeonGeneratorConfig extends DungeonGeneratorConfig(
  targetCount = 1,
  preDoorRoomCount = 2,
  mutators = Set(
//    CreateBossRoomMutator,
    DoorKeyLockMutator,
    CreateRoomMutator,
//    DoorSwitchLockMutator,
//    TeleporterMutator,
//    TreasureRoomMutator
  ),
  nodeFinders = Set(
    AdjacentRoomNodeFinder,
    RoomKeyFinder,
//    RoomSwitchFinder,
//    TeleporterRoomNodeFinder,
//    RoomBossKeyFinder
  ),
  predicates = Set(
    KeyCountPredicate(2),
//    NonPathRoomPredicate(1),
//    SwitchCountPredicate(2),
//    RoomCountPredicate(10),
//    new EntityCount[BossRoom.type](1),
//    LongestRoomPathPredicate(10),
//    TreasurePredicate(2),
  )
)
