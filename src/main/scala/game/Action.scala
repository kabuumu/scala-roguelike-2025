package game

trait Action {
  def apply(entity: Entity, gameState: GameState): GameState
}

case class Move(direction: Direction) extends Action {
  def apply(entity: Entity, gameState: GameState): GameState = {
    val movedEntity = entity.move(direction)
    gameState.copy(entities = gameState.entities - entity + movedEntity)

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
      entity.id,
      movedEntity.updateSightMemory(gameState)
    )
  }
}

case class Attack (cursorX: Int, cursorY: Int) extends Action {
  def apply(entity: Entity, gameState: GameState): GameState = {
    val enemy = gameState.getEnemy(cursorX, cursorY)
    enemy match {
      case Some(enemy) =>
        val newEnemy = enemy.copy(health = enemy.health - 1)
        if (newEnemy.health <= 0) {
          gameState.remove(enemy)
        } else {
          gameState.updateEntity(enemy.id, newEnemy)
        }
      case _ => gameState
    }
  }
}