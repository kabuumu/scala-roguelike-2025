package game

trait Action {
  def apply(entity: Entity, gameState: GameState): GameState
}

case class MoveAction(direction: Direction) extends Action {
  def apply(movingEntity: Entity, gameState: GameState): GameState = {
    val movedEntity = movingEntity.move(direction)
    gameState.copy(entities = gameState.entities - movingEntity + movedEntity)

    if (
      gameState.entities.exists(
        entity =>
          entity.xPosition == movedEntity.xPosition
            &&
            entity.yPosition == movedEntity.yPosition
            &&
            entity.entityType == EntityType.Wall
      )
    ) {
      gameState
    } else gameState.updateEntity(
      movingEntity.id,
      movedEntity.updateSightMemory(gameState)
    )
  }
}

case class AttackAction(cursorX: Int, cursorY: Int) extends Action {
  def apply(attackingEntity: Entity, gameState: GameState): GameState = {
    gameState.getActor(cursorX, cursorY) match {
      case Some(enemy) =>
        val newEnemy = enemy.copy(health = enemy.health - 1)
        if (newEnemy.health <= 0) {
          gameState
            .remove(enemy)
            .updateEntity(
              attackingEntity.id,
              attackingEntity.copy(initiative = attackingEntity.INITIATIVE_MAX)
            )
        } else {
          gameState
            .updateEntity(enemy.id, newEnemy)
            .updateEntity(
              attackingEntity.id,
              attackingEntity.copy(initiative = attackingEntity.INITIATIVE_MAX)
            )
        }
      case _ =>
        throw new Exception(s"No target found at $cursorX, $cursorY")
    }
  }
}