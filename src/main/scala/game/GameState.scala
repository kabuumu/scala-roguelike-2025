package game

import game.EntityType.LockedDoor
import map.Dungeon

case class GameState(playerEntityId: String, entities: Seq[Entity], messages: Seq[String] = Nil, dungeon: Dungeon) {
  private val framesPerSecond = 8

  val playerEntity: Entity = entities.find(_.id == playerEntityId).get

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
        case (gameState, entity) if entity.initiative <= 0 && entity.entityType == EntityType.Enemy && !entity.isDead =>
          val nextAction = EnemyAI.getNextAction(entity, gameState)
          nextAction.apply(entity, gameState)

        case (gameState, entity) if entity.entityType == EntityType.Enemy || entity.entityType == EntityType.Player =>
          gameState.updateEntity(entity.id, entity.copy(initiative = entity.initiative - 1))
        case (gameState, _) =>
          gameState
      }
    }
  }

  def updateEntity(entityId: String, newEntity: Entity): GameState =
    copy(entities = entities.updated(entities.indexWhere(_.id == entityId), newEntity))

  def getEntity(x: Int, y: Int): Option[Entity] = {
    entities.find(entity => entity.xPosition == x && entity.yPosition == y)
  }

  def getActor(x: Int, y: Int): Option[Entity] = {
    entities.find(entity => entity.xPosition == x && entity.yPosition == y && (entity.entityType == EntityType.Enemy || entity.entityType == EntityType.Player))
  }

  def remove(entity: Entity): GameState = {
    copy(entities = entities.filterNot(_.id == entity.id))
  }

  lazy val playerVisiblePoints: Set[Point] = getVisiblePointsFor(playerEntity)

  private def getVisiblePointsFor(entity: Entity): Set[Point] = {
    entity.getLineOfSight(this)
  }

  def addMessage(message: String): GameState = {
    copy(messages = message +: messages)
  }

  val lineOfSightBlockingPoints: Set[Point] =
    dungeon.walls ++
      entities.collect {
        case entity if entity.lineOfSightBlocking && !entity.isDead =>
          entity.position
      }.toSet

  val movementBlockingPoints: Set[Point] =
    dungeon.walls ++
      entities.collect {
        case entity if !entity.isDead && (entity.entityType == EntityType.Enemy || entity.entityType.isInstanceOf[LockedDoor]) =>
          entity.position
      }.toSet
}