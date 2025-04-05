package dungeongenerator.pathfinder

import dungeongenerator.generator.Entity._
import dungeongenerator.generator.{DefaultDungeonGeneratorConfig, Point}
import dungeongenerator.pathfinder.nodefinders.NodeFinder
import dungeongenerator.pathfinder.DungeonCrawlerAction._

import scala.annotation.tailrec

object PathFinder {
  type Path = Seq[Node]
  type TargetNodePredicate = Node => Boolean
  type PathFailurePredicate = Path => Boolean

  def findPath(startingNode: Node,
               targetNodePredicate: TargetNodePredicate,
               pathFailureTriggers: Set[PathFailurePredicate],
               nodeFinders: Iterable[NodeFinder]): Path = {
    //    val currentTime = System.currentTimeMillis()
    val path = findPath(Set(Seq(startingNode)), Set.empty, targetNodePredicate, pathFailureTriggers, nodeFinders)
    //    val endTime = System.currentTimeMillis()
    //    println(s"Pathfinding took ${endTime - currentTime} milliseconds and produced a path of ${path.size} length using $nodeFinders.")
    path
  }

  @tailrec
  private def findPath(openPaths: Iterable[Path],
                       successfulPaths: Iterable[Path],
                       targetNodePredicate: TargetNodePredicate,
                       pathFailureTriggers: Set[PathFailurePredicate],
                       nodeFinders: Iterable[NodeFinder],
                       iteration: Int = 0): Path = {
    if (openPaths.exists(openPath => pathFailureTriggers.exists(_.apply(openPath)))) {
      Nil // TODO: Add proper failure path state
    } else if (openPaths.isEmpty || iteration > 20000) {
      successfulPaths.minByOption(_.size)
        .getOrElse(Nil) //TODO - make pathfinder return optional path for when one can't be found
    } else {
      val newSuccessfulPaths = openPaths.filter(path => targetNodePredicate(path.last))

      val updatedPaths = for {
        nodeFinder <- nodeFinders
        openPath <- openPaths
        newNode <- nodeFinder.getPossibleNodes(openPath.last)
        if !(openPaths ++ successfulPaths).exists(_.contains(newNode))
      } yield openPath :+ newNode

      val curatedUpdatedPaths = updatedPaths.groupBy(_.last).map(_._2.head)
      findPath(curatedUpdatedPaths, successfulPaths ++ newSuccessfulPaths, targetNodePredicate, pathFailureTriggers, nodeFinders, iteration + 1)

    }
  }

  def locationPredicate(point: Point): Node => Boolean = _.currentCrawler.location == point

  val keysUsedPredicate: Node => Boolean = _.currentCrawler.inventory.count(_ == Key) == 0

  val allDoorsUnlockedPredicate: Node => Boolean = _.dungeonState.entities.collectFirst { case (_, Door(Some(_))) => }.isEmpty

  val hasKeysFailureCase: PathFailurePredicate = _.last.currentCrawler.inventory.contains(Key)

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
                println("This shouldn't happen")
                false
            }
          }

          hasAlreadySeen
      }

    !allHaveBeenSeen //return true (fail) if any of the switches fall before their target
  }

  val getKeyBeforeLockedDoorFailureCase: PathFailurePredicate = path => {
    val currentCrawler = path.last.currentCrawler

    if (currentCrawler.lastAction == PickedUpKey) {
      val keyCount = currentCrawler.inventory.count(_ == Key)
      val lockedDoors = path.last.dungeonState.entities.filter(_._2 == Door(Some(KeyLock)))

      val lockedDoorRoomLocations: Set[Point] = lockedDoors.flatMap {
        case (lockedDoorLocation, _) => path.last.dungeonState.entities.collect {
          case (roomPoint, room: Room) if room.entities.exists(_._1 == lockedDoorLocation) =>
            roomPoint
        }
      }

      val seenLockedDoorCount = path.map(_.currentCrawler.location).toSet.count(lockedDoorRoomLocations.contains)

      keyCount > seenLockedDoorCount
    } else false
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