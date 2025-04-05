package dungeongenerator.generator.mutators

import dungeongenerator.generator.Entity._
import dungeongenerator.generator.{Dungeon, DungeonGeneratorConfig, Entity, Point}

case object CreateRoomMutator extends DungeonMutator {
  val defaultMinRoomSize = 8
  val defaultMaxRoomSize = 16
  val interval = 8

  override def getPossibleMutations(dungeon: Dungeon, config: DungeonGeneratorConfig): Iterable[Dungeon] = {
    if (dungeon.entities.isEmpty) {
      Some(dungeon ++ initialRoomEntities)
    } else {
      createAdditionalRooms(dungeon, defaultMinRoomSize, defaultMaxRoomSize).iterator.toSet.map {
        (newRoom: PotentialRoom) =>
          dungeon.copy(entities =
            (dungeon.entities ++ newRoom.entities)
              .filterNot { case (point, _) => point == newRoom.intersectPoint } + (newRoom.intersectPoint -> Door(None))
          )
      }
    }
  }

  def createAdditionalRooms(dungeon: Dungeon, minRoomSize: Int = defaultMinRoomSize, maxRoomSize: Int = defaultMaxRoomSize): Iterable[PotentialRoom] =
    for {
      xSize <- minRoomSize to maxRoomSize
      if xSize % interval == 0
      ySize <- minRoomSize to maxRoomSize
      if ySize % interval == 0
      originX <- Math.max(0, dungeon.minPoint.x - xSize) to Math.min((Dungeon.dungeonSize - 1) * xSize, dungeon.maxPoint.x)
      if originX % interval == 0
      originY <- Math.max(0, dungeon.minPoint.y - ySize) to Math.min((Dungeon.dungeonSize - 1) * ySize, dungeon.maxPoint.y)
      if originY % interval == 0
      maxX = originX + xSize
      maxY = originY + ySize
      newRoom = createRoom(originX, originY, maxX, maxY)
      centerX = originX + (xSize / 2)
      centerY = originY + (ySize / 2)
      intersectPoint <- dungeon.entities.filter(_._2 == Wall)
        .intersect(newRoom.filter(_._2 == Wall))
        .collectFirst {
          case (point, _) if point.x % interval == interval / 2 || point.y % interval == interval / 2 => point
        }
      if newRoom.map(_._1).intersect(dungeon.entities.collect { case (point, Floor) => point }).isEmpty
    } yield {
      PotentialRoom(
        walls = newRoom.collect { case (point, Wall) => point },
        floors = newRoom.collect { case (point, Floor) => point },
        center = Point(centerX, centerY),
        intersectPoint = intersectPoint
      )
    }

  //Creates initial room if there are no rooms
  //TODO - Move this out into a separate mutator
  private val initialRoomEntities: Set[(Point, Entity)] = {
    val roomSize = interval

    val originX = (Dungeon.dungeonSize / 2) * roomSize
    val originY = (Dungeon.dungeonSize / 2) * roomSize
    val centerX = originX + (roomSize / 2)
    val centerY = originY + (roomSize / 2)

    createRoom(originX, originY, originX + roomSize, originY + roomSize) + (Point(centerX, centerY) -> StartPoint)
  }

  private def createRoom(originX: Int, originY: Int, maxX: Int, maxY: Int): Set[(Point, Entity)] = {
    val roomPoint = Point((originX + maxX) / 2, (originY + maxY) / 2)

    val roomPoints: Iterable[(Point, Entity)] = for {
      x <- originX to maxX
      y <- originY to maxY
      entityType = if (x == originX || x == maxX || y == originY || y == maxY) Wall else Floor
    } yield Point(x, y) -> entityType

    roomPoints.toSet + (roomPoint -> Room(roomPoints))
  }

  case class PotentialRoom(walls: Set[Point],
                           floors: Set[Point],
                           center: Point,
                           intersectPoint: Point) {
    val entities: Set[(Point, Entity)] = {
      val roomEntities: Set[(Point, Entity)] =
        Set.empty[(Point, Entity)] ++ walls.map(_ -> Wall) ++ floors.map(_ -> Floor)
      roomEntities + (center -> Room(roomEntities))
    }
  }

}
