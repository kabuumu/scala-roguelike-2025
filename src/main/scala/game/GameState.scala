package game

case class GameState(playerEntityId: String, entities: Set[Entity]) {
  private val framesPerSecond = 8

  val playerEntity: Entity = entities.find(_.id == playerEntityId).get

  def update(playerAction: Option[Action]): GameState = {
    playerAction match {
      case Some(Action.Move(direction)) =>
        val newPlayerEntity = playerEntity.move(direction)
        if (
          entities.exists(
            entity =>
              entity.xPosition == newPlayerEntity.xPosition
                &&
                entity.yPosition == newPlayerEntity.yPosition
                &&
                entity.entityType == EntityType.Wall
          )
        ) {
          this
        } else update(playerEntity, newPlayerEntity)
      case Some(game.Action.Attack(cursorX, cursorY)) =>
        getEntity(cursorX, cursorY) match {
          case Some(enemy) if enemy.entityType == EntityType.Enemy =>
            enemy.copy(health = enemy.health - 1) match {
              case newEnemy if newEnemy.health <= 0 => remove(enemy)
              case newEnemy => update(enemy, newEnemy)
            }
          case _ => this
        }
      case None => this
    }
  }

  def update(entity: Entity, newEntity: Entity): GameState = {
    copy(entities = entities - entity + newEntity)
  }

  def getEntity(x: Int, y: Int): Option[Entity] = {
    entities.find(entity => entity.xPosition == x && entity.yPosition == y)
  }

  def remove(entity: Entity): GameState = {
    copy(entities = entities - entity)
  }
}