package game

import game.Item.Item

import java.util.UUID

case class Entity(
                   id: String = UUID.randomUUID().toString,
                   position: Point,
                   entityType: EntityType,
                   health: Health,
                   lineOfSightBlocking: Boolean = false,
                   sightMemory: Set[Point] = Set.empty,
                   initiative: Int = 0,
                   isDead: Boolean = false,
                   inventory: Inventory = Inventory()
                 ) {
  val INITIATIVE_MAX: Int = entityType match {
    case EntityType.Player => 5
    case EntityType.Enemy => 6
    case _ => 0
  }

  def move(direction: Direction): Entity = {
    copy(
      position = position + direction,
      initiative = INITIATIVE_MAX
    )
  }

  def updateSightMemory(gameState: GameState): Entity = copy(
    //TODO - this will cause problems for entities that move - possibly need different rules for those
    sightMemory = sightMemory ++ getLineOfSight(gameState)
  )


  
  def getLineOfSight(gameState: GameState): Set[Point] = {
    LineOfSight.getVisiblePoints(
      position,
      gameState.lineOfSightBlockingPoints,
      sightRange = 10
    )
  }

  def canSee(gameState: GameState, otherEntity: Entity): Boolean = {
    getLineOfSight(gameState).contains(otherEntity.position)
  }

  val name: String = entityType.toString
}
