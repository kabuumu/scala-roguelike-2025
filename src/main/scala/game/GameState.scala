package game

import game.entity.*
import game.entity.ActorController.*
import game.entity.EntityType.LockedDoor
import game.entity.Initiative.*
import map.Dungeon

case class GameState(playerEntityId: String,
                     entities: Seq[Entity],
                     messages: Seq[String] = Nil,
                     dungeon: Dungeon,
                     projectiles: Seq[Projectile] = Nil) {
  val playerEntity: Entity = entities.find(_.id == playerEntityId).get

  def update(playerAction: Option[Action]): GameState = {
    playerAction match {
      case Some(action) if playerEntity.isReady =>
        action.apply(playerEntity, this)
      case _ => this
    }
  }

  def update(): GameState = {
    if (playerEntity.isReady) {
      this // wait for player to act
    } else {
      val entityUpdated = entities.foldLeft(this) {
        case (gameState, entity) =>
          entity.update(gameState)
      }

      val projectileUpdated = projectiles.foldLeft(entityUpdated) {
        case (gameState, projectile) =>
          projectile.update(gameState)
      }

      projectileUpdated
    }
  }

  def updateEntity(entityId: String, newEntity: Entity): GameState =
    copy(entities = entities.updated(entities.indexWhere(_.id == entityId), newEntity))

  def updateEntity(entityId: String, update: Entity => Entity): GameState =
    copy(
      entities = entities.updated(entities.indexWhere(_.id == entityId), update(entities.find(_.id == entityId).get))
    )

  def getActor(point: Point): Option[Entity] = {
    entities.find(entity => entity.exists[Movement](_.position == point) && (entity.exists[EntityTypeComponent](entityType => entityType.entityType == EntityType.Enemy || entityType.entityType == EntityType.Player)))
  }

  def add(entity: Entity): GameState = {
    copy(entities = entities :+ entity)
  }

  def remove(entity: Entity): GameState = {
    copy(entities = entities.filterNot(_.id == entity.id))
  }

  lazy val playerVisiblePoints: Set[Point] = getVisiblePointsFor(playerEntity)


  //TODO - remove magic number
  def getVisiblePointsFor(entity: Entity): Set[Point] = for {
    entityPosition <- entity.get[Movement].map(_.position).toSet
    lineOfSight <- LineOfSight.getVisiblePoints(entityPosition, lineOfSightBlockingPoints, 10)
  } yield lineOfSight

  def addMessage(message: String): GameState = {
    copy(messages = message +: messages)
  }

  val lineOfSightBlockingPoints: Set[Point] = dungeon.walls ++
      entities.collect {
        case entity@Entity[Movement](movement)if entity.exists[EntityTypeComponent](_.entityType.isInstanceOf[EntityType.LockedDoor])=>
        movement.position
      }.toSet

  val movementBlockingPoints: Set[Point] = dungeon.walls ++
      entities.collect {
        case entity@Entity[Movement](movement) if (entity.exists[Health](_.isAlive) && entity.exists[EntityTypeComponent](_.entityType == EntityType.Enemy)) || entity.exists[EntityTypeComponent](_.entityType.isInstanceOf[LockedDoor]) =>
        movement.position
      }.toSet


  val drawableChanges: Seq[Point] = {
    entities.flatMap(_.get[Movement].map(_.position)) ++ projectiles.map(_.position)
  }
}
