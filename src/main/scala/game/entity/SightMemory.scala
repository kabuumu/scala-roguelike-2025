package game.entity

import game.{GameState, Point}

case class SightMemory(seenPoints: Set[Point] = Set.empty) extends Component {
  def update(gameState: GameState, entity: Entity): SightMemory = {
    val updatedSeenPoints = seenPoints ++ gameState.getVisiblePointsFor(entity)
    copy(updatedSeenPoints)
  }
}

