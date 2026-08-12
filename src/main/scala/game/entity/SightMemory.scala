package game.entity

import game.{GameState, Point}

case class SightMemory(seenPoints: Set[Point] = Set.empty) extends Component {
  def update(gameState: GameState, entity: Entity): SightMemory = {
    val visible = gameState.getVisiblePointsFor(entity)
    if (visible.forall(seenPoints.contains)) {
      this
    } else {
      copy(seenPoints ++ visible)
    }
  }
}

object SightMemory {
  extension (entity: Entity) {
    def updateSightMemory(gameState: GameState): Entity = {
      entity.update[SightMemory](_.update(gameState, entity))
    }
  }
}

