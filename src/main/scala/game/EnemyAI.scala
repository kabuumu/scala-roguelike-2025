package game

object EnemyAI {
  def getNextAction(enemy: Entity, gameState: GameState): Action = {
    //TODO - Make target persistent
    val target = gameState.playerEntity

    if (enemy.position.isWithinRangeOf(target.position, 1)) {
      AttackAction(target.position)
    } else if(enemy.canSee(gameState, target)) {
      getMoveAction(enemy, target, gameState)
    } else {
      WaitAction
    }
  }

  private def getMoveAction(enemy: Entity, target: Entity, gameState: GameState): Action = {
    val path = Pathfinder.findPath(
      enemy.position,
      target.position,
      (gameState.movementBlockingPoints - target.position).toSeq
    )

    path.drop(1).headOption match {
      case Some(nextStep) =>
        val direction = Direction.fromPoints(
          enemy.position,
          nextStep
        )

        MoveAction(direction)
      case None =>
        WaitAction
    }
  }
}
