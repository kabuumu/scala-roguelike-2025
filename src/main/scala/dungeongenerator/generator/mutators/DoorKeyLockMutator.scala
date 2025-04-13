package dungeongenerator.generator.mutators

import dungeongenerator.generator.Entity.*
import dungeongenerator.generator.{Dungeon, DungeonGeneratorConfig}
import dungeongenerator.pathfinder.DungeonCrawlerAction.Moved
import dungeongenerator.pathfinder.{DungeonCrawler, Node, PathFinder}

case object DoorKeyLockMutator extends DungeonMutator {
  override def getPossibleMutations(dungeon: Dungeon, config: DungeonGeneratorConfig): Iterable[Dungeon] =
    for {
      longestPathEndNode <- dungeon.longestRoomPath.lastOption.toSeq
      (previousRoom, doorToLock) <- longestPathEndNode.currentCrawler.actions.collect {
        case Moved(previousRoom, door, _) if dungeon.entities.exists { case (position, entity) => position == door && entity == Door(None) } =>
          (previousRoom, door)
      }
      keyRoom <- CreateRoomMutator.createAdditionalRooms(dungeon, maxRoomSize = 8)
      directPath = PathFinder.findPath(
        startingNode = Node(
          DungeonCrawler(keyRoom.center),
          dungeon + keyRoom
        ),
        targetNodePredicate = PathFinder.locationPredicate(previousRoom),
        pathFailureTriggers = Set.empty,
        nodeFinders = config.nodeFinders
      )
      if directPath.nonEmpty // Ensure that the path is not empty
    } yield {
      val lockedDoor = doorToLock -> Door(Some(ItemLock(Key(KeyColour.Yellow))))

      dungeon + keyRoom + (keyRoom.center -> Key(KeyColour.Yellow)) - (doorToLock -> Door(None)) + lockedDoor
    }
}
