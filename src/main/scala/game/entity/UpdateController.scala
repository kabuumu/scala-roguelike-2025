package game.entity

import game.GameState
import game.entity.EntityType.*

case class UpdateController(updateActions: UpdateAction*) extends Component {
  private def update(gameState: GameState, entity: Entity): GameState = {
    updateActions.foldLeft(gameState) {
      case (state, action) =>
        action(entity, state)
    }
  }
}

object UpdateController {
  extension (entity: Entity) {
    def update(gameState: GameState): GameState = {
      entity.get[UpdateController] match {
        case Some(controller) =>
          controller.update(gameState, entity)
        case None =>
          gameState
      }
    }
  }
}
