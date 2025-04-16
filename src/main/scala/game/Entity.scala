package game

import game.Item.Item

import java.util.UUID

case class Entity(
                   id: String = UUID.randomUUID().toString,
                   xPosition: Int,
                   yPosition: Int,
                   entityType: EntityType,
                   health: Health,
                   lineOfSightBlocking: Boolean = false,
                   sightMemory: Set[Point] = Set.empty,
                   initiative: Int = 0,
                   isDead: Boolean = false,
                   inventory: Seq[Item] = Nil
                 ) {
  val INITIATIVE_MAX = entityType match {
    case EntityType.Player => 10
    case EntityType.Enemy => 12
    case _ => 0
  }

  val position: Point = Point(xPosition, yPosition)

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
      gameState.blockingPoints,
      sightRange = 10
    )
  }

  def canSee(gameState: GameState, otherEntity: Entity): Boolean = {
    getLineOfSight(gameState).contains(otherEntity.position)
  }

  val name: String = entityType.toString
}
