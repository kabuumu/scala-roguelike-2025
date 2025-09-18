package game.entity

import game.{GameState, Point}
import upickle.default.ReadWriter

case class SightMemory(seenPoints: Set[Point] = Set.empty) extends Component derives ReadWriter {
  def update(gameState: GameState, entity: Entity): SightMemory = {
    val updatedSeenPoints = seenPoints ++ gameState.getVisiblePointsFor(entity)
    copy(updatedSeenPoints)
  }
}

object SightMemory {
  extension (entity: Entity) {
    def updateSightMemory(gameState: GameState): Entity = {
      entity.update[SightMemory](_.update(gameState, entity))
    }
  }
}

