package game.entity

import game.GameState
import game.entity.EntityType.*
import game.event.Event

case class UpdateController(updateActions: UpdateAction*) extends Component {
  private def update(entity: Entity, gameState: GameState): Seq[Event] = {
    updateActions.flatMap(_.apply(entity, gameState))
  }
}

object UpdateController {
  extension (entity: Entity) {
    def update(gameState: GameState): Seq[Event] =
      entity.get[UpdateController] match {
        case Some(controller) =>
          controller.update(entity, gameState)
        case None =>
          Nil
      }
  }
}
