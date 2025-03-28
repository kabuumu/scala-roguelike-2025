package game

case class GameState(playerEntityId: String, entities: Set[Entity], messages: Seq[String] = Nil) {
  private val framesPerSecond = 8

  val playerEntity: Entity = entities.find(_.id == playerEntityId).get

  val walls: Set[Point] = entities.collect {
    case entity if entity.entityType == EntityType.Wall =>
      Point(entity.xPosition, entity.yPosition)
  }

  def update(playerAction: Option[Action]): GameState = {
    playerAction match {
      case Some(action) if playerEntity.initiative == 0 =>
        action.apply(playerEntity, this)
      case _ => this
    }
  }

  def update(): GameState = {
    if (playerEntity.initiative == 0) {
      this // wait for player to act
    } else {
      entities.foldLeft(this) {
        case (gameState, entity) if entity.initiative <= 0 && entity.entityType == EntityType.Enemy  && !entity.isDead =>
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

  def getActor(x: Int, y: Int): Option[Entity] = {
    entities.find(entity => entity.xPosition == x && entity.yPosition == y && (entity.entityType == EntityType.Enemy || entity.entityType == EntityType.Player))
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

  def addMessage(message: String): GameState = {
    copy(messages = message +: messages)
  }
}