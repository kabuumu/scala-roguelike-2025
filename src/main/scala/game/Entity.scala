package game

import java.util.UUID

case class Entity(
                   id: String = UUID.randomUUID().toString,
                   xPosition: Int,
                   yPosition: Int,
                   entityType: EntityType,
                   health: Int,
                   lineOfSightBlocking: Boolean = false,
                   sightMemory: Set[Entity] = Set.empty
                 ) {
  def move(direction: Direction): Entity = {
    copy(
      xPosition = xPosition + direction.x,
      yPosition = yPosition + direction.y
    )
  }
}
