package game.entity

import game.GameState
import game.entity.EntityType.*

case class ActorController(updateActions: UpdateAction*) extends Component {
  private def update(gameState: GameState, entity: Entity): GameState = {
    updateActions.foldLeft(gameState) {
      case (state, action) =>
        action(entity, state)
    }
  }
}

object ActorController {
  extension (entity: Entity) {
    def update(gameState: GameState): GameState = {
      entity.get[ActorController] match {
        case Some(controller) =>
          controller.update(gameState, entity)
        case None =>
          gameState
      }
    }
  }
}
