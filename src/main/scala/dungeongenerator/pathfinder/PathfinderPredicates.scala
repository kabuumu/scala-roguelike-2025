package dungeongenerator.pathfinder

import dungeongenerator.generator.Entity.*
import dungeongenerator.generator.{DefaultDungeonGeneratorConfig, Point}
import dungeongenerator.pathfinder.DungeonCrawlerAction.*
import dungeongenerator.pathfinder.PathFinder.{PathFailurePredicate, TargetNodePredicate, findPath}

object PathfinderPredicates {

  def locationPredicate(point: Point): Node => Boolean = _.currentCrawler.location == point

  val hasKeysFailureCase: PathFailurePredicate = _.last.currentCrawler.inventory.exists(_.isInstanceOf[Key])

  val skippedDoorFailureCase: PathFailurePredicate = _.last.dungeonState.entities.collectFirst { case (_, Door(Some(_))) => }.isDefined

  def uncompletablePathFailureCase(targetNodePredicate: TargetNodePredicate,
                                   existingPathFailureTriggers: Set[PathFailurePredicate]): PathFailurePredicate = path => {
    val node = path.last
    if (node.currentCrawler.lastAction == UnlockedDoor) {
      val path = findPath(node, targetNodePredicate, existingPathFailureTriggers, DefaultDungeonGeneratorConfig.nodeFinders)

      path.isEmpty
    } else {
      false
    }
  }

  val skippedTeleporterFailureCase: PathFailurePredicate = path => {
    path.count(_.currentCrawler.lastAction == Teleported) < path.head.dungeonState.entities.count(_._2.isInstanceOf[Teleporter])
  }

  val switchBeforeLockedDoorFailureCase: PathFailurePredicate = path => {
    val allHaveBeenSeen = path
      .filter(_.currentCrawler.lastAction == ActivatedSwitch)
      .forall {
        switchNode =>
          val switchNodeIndex = path.indexOf(switchNode)
          val previousNode = path.apply(switchNodeIndex - 1) //Will fail if first node (shouldn't be possible)
          val dungeonChange = switchNode.dungeonState.entities.diff(previousNode.dungeonState.entities)

          val hasAlreadySeen = dungeonChange.forall {
            case (changedPoint, _) => path.last.dungeonState.entities.collectFirst {
              case (roomPoint, room: Room) if room.entities.exists(_._1 == changedPoint) =>
                roomPoint
            } match {
              case Some(roomPoint) =>
                path.indexWhere(_.currentCrawler.location == roomPoint) < switchNodeIndex
              case None =>
                false
            }
          }

          hasAlreadySeen
      }

    !allHaveBeenSeen //return true (fail) if any of the switches fall before their target
  }

  val getKeyBeforeLockedDoorFailureCase: PathFailurePredicate = path => {
    val currentCrawler = path.last.currentCrawler

    currentCrawler.lastAction match {
      case PickedUpKey(keyColour) =>
        val keyCount = currentCrawler.inventory.count(_ == Key(keyColour))
        val lockedDoors = path.last.dungeonState.entities.filter(_._2 == Door(Some(ItemLock(Key(keyColour)))))

        val lockedDoorRoomLocations: Set[Point] = lockedDoors.flatMap {
          case (lockedDoorLocation, _) => path.last.dungeonState.entities.collect {
            case (roomPoint, room: Room) if room.entities.exists(_._1 == lockedDoorLocation) =>
              roomPoint
          }
        }

        val seenLockedDoorCount = path.map(_.currentCrawler.location).toSet.count(lockedDoorRoomLocations.contains)

        keyCount > seenLockedDoorCount
      case _ => false
    }
  }

  val missedRoomFailureCase: PathFailurePredicate = path => {
    val rooms = path.last.dungeonState.entities.filter(_._2.isInstanceOf[Room])
    val visitedEachRoom = rooms.forall {
      case (roomPoint, _) =>
        path.exists(_.currentCrawler.location == roomPoint)
    }
    !visitedEachRoom
  }
}