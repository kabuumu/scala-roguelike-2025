package game

import java.util.UUID

case class Entity(
                   id: String = UUID.randomUUID().toString,
                   xPosition: Int,
                   yPosition: Int,
                   entityType: EntityType,
                   health: Int,
                   lineOfSightBlocking: Boolean = false,
                   sightMemory: Set[Point] = Set.empty,
                   initiative: Int = 0
                 ) {
  val INITIATIVE_MAX = 10

  def move(direction: Direction): Entity = {
    copy(
      xPosition = xPosition + direction.x,
      yPosition = yPosition + direction.y,
      initiative = INITIATIVE_MAX
    )
  }

  def updateSightMemory(gameState: GameState): Entity = copy(
    //TODO - this will cause problems for entities that move - possibly need different rules for those
    sightMemory = sightMemory ++ getLineOfSight(gameState)
  )

  def getLineOfSight(gameState: GameState): Set[Point] = {
    LineOfSight.getVisiblePoints(
      Point(xPosition, yPosition),
      gameState.entities.collect {
        case entity if entity.lineOfSightBlocking =>
          Point(entity.xPosition, entity.yPosition)
      },
      sightRange = 10
    )
  }
}
