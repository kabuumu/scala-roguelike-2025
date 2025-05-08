package game

import game.entity.*

trait EnemyAI {
  def getNextAction(enemy: Entity, gameState: GameState): Action
}

object EnemyAI {
  // TODO - Create more AI options in the future
  case object DefaultAI extends EnemyAI {
    def getNextAction(enemy: Entity, gameState: GameState): Action = {
      //TODO - Make target persistent
      val target = gameState.playerEntity
      val attackRange = enemy.get[Inventory].flatMap(_.primaryWeapon.map(_.range)).getOrElse(1) //TODO allow enemies to have secondary weapons?

      if (gameState.getVisiblePointsFor(enemy).contains(target[Movement].position)) {
        if (enemy[Movement].position.isWithinRangeOf(target[Movement].position, attackRange)) {
          AttackAction(target[Movement].position, enemy.get[Inventory].flatMap(_.primaryWeapon))
        } else getMoveAction(enemy, target, gameState)
      } else {
        WaitAction
      }
    }
  }

  private def getMoveAction(enemy: Entity, target: Entity, gameState: GameState): Action = {
    val path = Pathfinder.findPath(
      enemy[Movement].position,
      target[Movement].position,
      (gameState.movementBlockingPoints - target[Movement].position).toSeq
    )

    path.drop(1).headOption match {
      case Some(nextStep) =>
        val direction = Direction.fromPoints(
          enemy[Movement].position,
          nextStep
        )

        MoveAction(direction)
      case None =>
        WaitAction
    }
  }
}
