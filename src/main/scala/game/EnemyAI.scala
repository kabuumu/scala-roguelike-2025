package game

object EnemyAI {
  def getNextAction(enemy: Entity, gameState: GameState): Action = {
    val target = gameState.playerEntity

    if (enemy.position.isWithinRangeOf(target.position, 1)) {
      AttackAction(target.xPosition, target.yPosition)
    }
    else {
      getMoveAction(enemy, target, gameState)
    }
  }

  def getMoveAction(enemy: Entity, target: Entity, gameState: GameState): MoveAction = {
    val path = Pathfinder.findPath(
      Point(enemy.xPosition, enemy.yPosition),
      Point(target.xPosition, target.yPosition),
      gameState.entities.collect {
        case entity if entity.entityType == EntityType.Wall =>
          Point(entity.xPosition, entity.yPosition)
      }
    )

    path.drop(1).headOption match {
      case Some(nextStep) =>
        val direction = Direction.fromPoints(
          Point(enemy.xPosition, enemy.yPosition),
          nextStep
        )

        MoveAction(direction)
      case None =>
        throw new Exception("No path found")
    }
  }
}
