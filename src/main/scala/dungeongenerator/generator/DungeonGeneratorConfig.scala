package dungeongenerator.generator

import dungeongenerator.generator.Entity._
import dungeongenerator.generator.mutators.CreateBossRoomMutator.*
import dungeongenerator.generator.mutators._
import dungeongenerator.generator.predicates._
import dungeongenerator.pathfinder.nodefinders.NodeFinder
import dungeongenerator.pathfinder.nodefinders.room._

case class DungeonGeneratorConfig(targetCount: Int,
                                  preDoorRoomCount: Int = 2,
                                  preBossRoomCount: Int = 2,
                                  mutators: Set[DungeonMutator],
                                  nodeFinders: Set[NodeFinder],
                                  predicates: Set[DungeonPredicate])

object DefaultDungeonGeneratorConfig extends DungeonGeneratorConfig(
  targetCount = 1,
  preDoorRoomCount = 1,
  mutators = Set(
//    CreateBossRoomMutator,
    CreateRoomMutator,
//    DoorKeyLockMutator,
//    DoorSwitchLockMutator,
//    TeleporterMutator,
  ),
  nodeFinders = Set(
    AdjacentRoomNodeFinder,
//    RoomKeyFinder,
//    RoomSwitchFinder,
//    TeleporterRoomNodeFinder,
//    RoomBossKeyFinder
  ),
  predicates = Set(
//    KeyCountPredicate(1),
//    SwitchCountPredicate(2),
    RoomCountPredicate(5),
//    new EntityCount[BossRoom.type](1)
  )
)
