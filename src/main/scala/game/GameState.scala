package game

import game.entity.*
import game.entity.EntityType.LockedDoor
import game.entity.UpdateController.*
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
    LegacyAISystem,
    MovementSystem,
    VelocitySystem,
    LegacyWaveSystem,
    ItemUseSystem,
    CollisionCheckSystem,
    LegacyCollisionSystem,
    InventorySystem,
    DeathHandlerSystem,
    InitiativeSystem,
    LevelUpSystem,
  )

  def updateWithSystems(events: Seq[GameSystemEvent]): GameState =
    systems.foldLeft((this, events)) {
      case ((currentState, currentEvents), system) =>
        val (newState, newEvents) = system.update(currentState, currentEvents)
        (newState, currentEvents ++ newEvents)
    }._1
  

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
        case entity@Entity[Movement
        ] (movement)
        if (entity.exists[Health](_.isAlive) && entity.exists[EntityTypeComponent](_.entityType == EntityType.Enemy)) ||(entity.exists[EntityTypeComponent](_.entityType.isInstanceOf[LockedDoor]))
        =>
        movement.position
      }.toSet


  val drawableChanges: Seq[Set[(Point, Sprite)]] = {
    import game.entity.Drawable.*
    entities.map(_.sprites)
  }
}
