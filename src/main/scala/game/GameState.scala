package game

import game.entity.*
import game.entity.EntityType.*
import game.event.*
import game.system.*
import game.system.event.GameSystemEvent.GameSystemEvent
import map.Dungeon

import scala.annotation.tailrec

case class GameState(playerEntityId: String,
                     entities: Seq[Entity],
                     messages: Seq[String] = Nil,
                     dungeon: Dungeon) {
  val playerEntity: Entity = entities.find(_.id == playerEntityId).get

  def getEntity(entityId: String): Option[Entity] = {
    entities.find(_.id == entityId)
  }

  val systems: Seq[GameSystem] = Seq(
    EnemyAISystem,
    MovementSystem,
    LegacyVelocitySystem,
    LegacyWaveSystem,
    LegacyItemUseSystem,
    WaitSystem,
    OpenDoorSystem,
    CollisionCheckSystem,
    AttackSystem,
    LegacyRangeCheckSystem,
    LegacyCollisionSystem,
    DamageSystem,
    InventorySystem,
    LegacyDeathHandlerSystem,
    InitiativeSystem,
    LevelUpSystem,
    SightMemorySystem,
  )

  def updateWithSystems(events: Seq[GameSystemEvent]): GameState = {
    systems.foldLeft((this, events)) {
      case ((currentState, currentEvents), system) =>
        val (newState, newEvents) = system.update(currentState, currentEvents)
        (newState, currentEvents ++ newEvents)
    }._1
  }


  @scala.annotation.tailrec
  @deprecated
  final def handleEvents(events: Seq[Event]): GameState = {
    if (events.isEmpty) this
    else {
      val (newGameState, newEvents) = events.head.apply(this)
      newGameState.handleEvents(newEvents ++ events.tail)
    }
  }

  def updateEntity(entityId: String, newEntity: Entity): GameState =
    copy(entities = entities.updated(entities.indexWhere(_.id == entityId), newEntity))

  def updateEntity(entityId: String, update: Entity => Entity): GameState = {
    copy(
      entities = entities.updated(entities.indexWhere(_.id == entityId), update(entities.find(_.id == entityId).get))
    )
  }

  def getActor(point: Point): Option[Entity] = {
    entities.find(entity => entity.exists[Movement](_.position == point) && (entity.exists[EntityTypeComponent](entityType => entityType.entityType == EntityType.Enemy || entityType.entityType == EntityType.Player)))
  }

  def add(entity: Entity): GameState = {
    copy(entities = entities :+ entity)
  }

  def remove(entityId: String): GameState = {
    copy(entities = entities.filterNot(_.id == entityId))
  }

  //TODO - remove magic number
  def getVisiblePointsFor(entity: Entity): Set[Point] = for {
    entityPosition <- entity.get[Movement].map(_.position).toSet
    lineOfSight <- LineOfSight.getVisiblePoints(entityPosition, lineOfSightBlockingPoints, 10)
  } yield lineOfSight

  def addMessage(message: String): GameState = {
    copy(messages = message +: messages)
  }

  lazy val lineOfSightBlockingPoints: Set[Point] = dungeon.walls ++ dungeon.rocks ++
    entities
      .filter(_.entityType.isInstanceOf[LockedDoor])
      .flatMap(_.get[Movement].map(_.position))
      .toSet

  lazy val movementBlockingPoints: Set[Point] = dungeon.walls ++ dungeon.water ++ dungeon.rocks ++
    entities
      .filter(entity => entity.entityType == EntityType.Enemy || entity.entityType == EntityType.Player || entity.entityType.isInstanceOf[LockedDoor])
      .flatMap(_.get[Movement].map(_.position))
      .toSet
  
  lazy val drawableChanges: Seq[Set[(Point, Sprite)]] = {
    import game.entity.Drawable.*
    entities.map(_.sprites)
  }
}
