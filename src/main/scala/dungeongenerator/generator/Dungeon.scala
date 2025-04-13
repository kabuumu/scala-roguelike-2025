package dungeongenerator.generator

import dungeongenerator.generator.Entity.*
import dungeongenerator.generator.mutators.CreateRoomMutator.PotentialRoom
import dungeongenerator.pathfinder.PathFinder.*
import dungeongenerator.pathfinder.{DungeonCrawler, Node}
import dungeongenerator.pathfinder.PathfinderPredicates.*

import scala.reflect.ClassTag

case class Dungeon(entities: Set[(Point, Entity)]) {
  val minPoint: Point = entities.minByOption {
    case (Point(x, y), _) => x + y
  } match {
    case Some((point, _)) => point
    case None => Point(0, 0)
  }

  val maxPoint: Point = entities.maxByOption {
    case (Point(x, y), _) => x + y
  } match {
    case Some((point, _)) => point
    case None => Point(0, 0)
  }

  val roomLocations: Set[Point] = entities.collect{
    case (point, room: Room) => point
  }

  val optStartPoint: Option[Point] = entities.collectFirst {
    case (point, StartPoint) => point
  }

  val roomCount: Int = entities.count { case (_, entity) => entity.isInstanceOf[Room] }
  val lockedDoorCount: Int = entities.collect { case (_, Door(Some(_))) => () }.size

  def count[T <: Entity](implicit tag: ClassTag[T]) = entities.count { case (_, entity) => tag.runtimeClass.isInstance(entity) }

  lazy val longestRoomPath: Seq[Node] = {
    val bossRoom = entities.collectFirst {
      case (point, BossRoom) => point
    } match {
      case Some(bossRoomPoint) => Seq(bossRoomPoint)
      case None => entities.collect {
        case (point, _: Room) => point
      }
    }
    optStartPoint match {
      case Some(startPoint) =>
        bossRoom.map {
          targetRoom =>
            val targetNodePredicate: TargetNodePredicate = locationPredicate(targetRoom)

            def completedDungeonAnd(pathFailurePredicate: PathFailurePredicate): PathFailurePredicate = path =>
              targetNodePredicate(path.last) && pathFailurePredicate(path)

            val standardPathFailureTriggers: Set[PathFailurePredicate] = Set(
              completedDungeonAnd(hasKeysFailureCase),
              completedDungeonAnd(skippedDoorFailureCase),
              completedDungeonAnd(skippedTeleporterFailureCase),
//              completedDungeonAnd(missedRoomFailureCase),
              switchBeforeLockedDoorFailureCase,
              getKeyBeforeLockedDoorFailureCase,
            )

            findPath(
              startingNode = Node(DungeonCrawler(startPoint), this),
              targetNodePredicate = targetNodePredicate,
              pathFailureTriggers = standardPathFailureTriggers + uncompletablePathFailureCase(targetNodePredicate, Set.empty),
              nodeFinders = DefaultDungeonGeneratorConfig.nodeFinders //TODO: have this be injected
            )
        }.maxBy(_.size)
      case None =>
        Nil
    }
  }

  def -(entity: (Point, Entity)): Dungeon = copy(entities = entities - entity)

  def +(entity: (Point, Entity)): Dungeon = copy(entities = entities + entity)

  def ++(newEntities: Iterable[(Point, Entity)]): Dungeon = copy(entities = entities ++ newEntities)

  def +(newRoom: PotentialRoom): Dungeon = {
    copy(entities =
      (entities ++ newRoom.entities)
        .filterNot { case (point, _) => point == newRoom.intersectPoint } + (newRoom.intersectPoint -> Door(None))
    )
  }

  val nonPathRooms: Set[Point] = roomLocations.filterNot { roomLocation =>
    longestRoomPath.exists(_.currentCrawler.location == roomLocation)
  }
}

object Dungeon {
  val empty: Dungeon = Dungeon(Set.empty)

  val dungeonSize = 7
}
