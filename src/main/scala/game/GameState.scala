package game

case class GameState(playerEntityId: String, entities: Set[Entity]) {
  private val framesPerSecond = 8

  val playerEntity: Entity = entities.find(_.id == playerEntityId).get

  def update(playerAction: Option[Action]): GameState = {
    playerAction match {
      case Some(Move(direction)) if playerEntity.initiative == 0 =>
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
      case Some(Attack(cursorX, cursorY)) if playerEntity.initiative == 0 =>
        println("processed action is: " + playerAction)

        getEnemy(cursorX, cursorY) match {
          case Some(enemy) =>
            println(s"Player attacks enemy: $enemy")
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
    } else {
      entities.foldLeft(this) {
        case (gameState, entity) if entity.initiative <= 0 && entity.entityType == EntityType.Enemy =>
          val nextAction = EnemyAI.getNextAction(entity, gameState)
          nextAction.apply(entity, gameState)

        case (gameState, entity) =>
          gameState.updateEntity(entity.id, entity.copy(initiative = entity.initiative - 1))
      }
    }
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

  def getEnemy(x: Int, y: Int): Option[Entity] = {
    entities.find(entity => entity.xPosition == x && entity.yPosition == y && entity.entityType == EntityType.Enemy)
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