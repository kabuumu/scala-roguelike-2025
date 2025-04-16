package game

object EnemyAI {
  def getNextAction(enemy: Entity, gameState: GameState): Action = {
    //TODO - Make target persistent
    val target = gameState.playerEntity

    if (enemy.position.isWithinRangeOf(target.position, 1)) {
      AttackAction(target.xPosition, target.yPosition)
    } else if(enemy.canSee(gameState, target)) {
      getMoveAction(enemy, target, gameState)
    } else {
      WaitAction
    }
  }

  private def getMoveAction(enemy: Entity, target: Entity, gameState: GameState): Action = {
    val path = Pathfinder.findPath(
      Point(enemy.xPosition, enemy.yPosition),
      Point(target.xPosition, target.yPosition),
      (gameState.blockingPoints - target.position).toSeq
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
