package game.entity

import game.{EnemyAI, GameState}
import Health._
import Initiative._
import EntityType._

case class Controller() extends Component {
  private def update(gameState: GameState, entity: Entity): GameState = {
    if entity.isAlive then
      if entity.isReady && entity.entityType == EntityType.Enemy then
        val nextAction = EnemyAI.getNextAction(entity, gameState)
        nextAction.apply(entity, gameState)
      else
        gameState.updateEntity(entity.id, entity.decreaseInitiative())
    else gameState
  }
}

object Controller {
  extension (entity: Entity) {
    def update(gameState: GameState): GameState = {
      entity.get[Controller] match {
        case Some(controller) =>
          controller.update(gameState, entity)
        case None =>
          gameState
      }
    }
  }
}
