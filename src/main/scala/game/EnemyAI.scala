package game

import game.EntityType.Wall

object EnemyAI {
  def getNextAction(enemy: Entity, gameState: GameState): Action = {
    val target = gameState.playerEntity

    if (enemy.position.isWithinRangeOf(target.position, 1)) {
      AttackAction(target.xPosition, target.yPosition)
    } else {
      getMoveAction(enemy, target, gameState)
    }
  }

  private def getMoveAction(enemy: Entity, target: Entity, gameState: GameState): Action = {
    val path = Pathfinder.findPath(
      Point(enemy.xPosition, enemy.yPosition),
      Point(target.xPosition, target.yPosition),
      gameState.movementBlockingEntities
        .filterNot(entity => entity.id == target.id)
        .map(_.position)
    )

    path.drop(1).headOption match {
      case Some(nextStep) =>
        val direction = Direction.fromPoints(
          Point(enemy.xPosition, enemy.yPosition),
          nextStep
        )

        MoveAction(direction)
      case None =>
        WaitAction
    }
  }
}
