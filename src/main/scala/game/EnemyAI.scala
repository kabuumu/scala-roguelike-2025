package game

object EnemyAI {
  def getNextAction(enemy: Entity, gameState: GameState): Action = {
    //TODO - Make target persistent
    val target = gameState.playerEntity
    val attackRange = enemy.inventory.primaryWeapon.map(_.range).getOrElse(1) //TODO allow enemies to have secondary weapons?

    if (enemy.canSee(gameState, target)) {
      if (enemy.position.isWithinRangeOf(target.position, attackRange)) {
        AttackAction(target.position, enemy.inventory.primaryWeapon)
      } else getMoveAction(enemy, target, gameState)
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
