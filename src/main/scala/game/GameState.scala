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
        } else update(
          playerEntity.id,
          newPlayerEntity.updateSightMemory(this)
        )
      case Some(game.Action.Attack(cursorX, cursorY)) =>
        getEntity(cursorX, cursorY) match {
          case Some(enemy) if enemy.entityType == EntityType.Enemy =>
            enemy.copy(health = enemy.health - 1) match {
              case newEnemy if newEnemy.health <= 0 => remove(enemy)
              case newEnemy => update(enemy.id, newEnemy)
            }
          case _ => this
        }
      case None => this
    }
  }

  def update(entityId: String, newEntity: Entity): GameState = {
    copy(entities = entities.map {
      case entity if entity.id == entityId => newEntity
      case other => other
    })
  }

  def getEntity(x: Int, y: Int): Option[Entity] = {
    entities.find(entity => entity.xPosition == x && entity.yPosition == y)
  }

  def remove(entity: Entity): GameState = {
    copy(entities = entities - entity)
  }

  def getVisibleEntitiesFor(entity: Entity): Set[Entity] = {
    val visiblePoints = entity.getLineOfSight(this)

    entities.filter{
      visibleEntity =>
        visiblePoints.exists(
          visiblePoint =>
            visibleEntity.xPosition == visiblePoint.x && visibleEntity.yPosition == visiblePoint.y
        )

    }

  }
}