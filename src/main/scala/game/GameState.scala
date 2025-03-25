package game

case class GameState(playerEntityId: String, entities: Set[Entity]) {
  private val framesPerSecond = 8

  val playerEntity: Entity = entities.find(_.id == playerEntityId).get

  def update(playerAction: Option[Action]): GameState = {
    playerAction match {
      case Some(Action.Move(direction)) if playerEntity.initiative == 0 =>
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
        } else updateEntity(
          playerEntity.id,
          newPlayerEntity.updateSightMemory(this)
        )
      case Some(game.Action.Attack(cursorX, cursorY)) if playerEntity.initiative == 0 =>
        getEntity(cursorX, cursorY) match {
          case Some(enemy) if enemy.entityType == EntityType.Enemy =>
            enemy.copy(health = enemy.health - 1) match {
              case newEnemy if newEnemy.health <= 0 => remove(enemy)
              case newEnemy => updateEntity(enemy.id, newEnemy)
            }
          case _ => this
        }
      case _ => this
    }
  }

  def update(): GameState = {
    if (playerEntity.initiative == 0) {
      this // wait for player to act
    } else copy(
      entities = entities
        .map(entity => entity.copy(initiative = entity.initiative - 1))
        .map {
          case entity if entity.initiative <= 0 && entity.entityType == EntityType.Enemy =>
            val path = Pathfinder.findPath(
              Point(entity.xPosition, entity.yPosition),
              Point(playerEntity.xPosition, playerEntity.yPosition),
              entities.collect {
                case entity if entity.entityType == EntityType.Wall =>
                  Point(entity.xPosition, entity.yPosition)
              }
            )


            path.drop(1).headOption match {
              case Some(nextStep) =>
                val direction = Direction.fromPoints(
                  Point(entity.xPosition, entity.yPosition),
                  nextStep
                )

                val newEnemy = entity.move(direction)
                if (
                  entities.exists(
                    entity =>
                      entity.xPosition == newEnemy.xPosition
                        &&
                        entity.yPosition == newEnemy.yPosition
                        &&
                        entity.entityType == EntityType.Wall
                  )
                ) {
                  entity
                } else newEnemy
              case None => entity
            }
          case entity => entity
        }
    )
  }

  def updateEntity(entityId: String, newEntity: Entity): GameState = {
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

    entities.filter {
      visibleEntity =>
        visiblePoints.exists(
          visiblePoint =>
            visibleEntity.xPosition == visiblePoint.x && visibleEntity.yPosition == visiblePoint.y
        )

    }
  }
}