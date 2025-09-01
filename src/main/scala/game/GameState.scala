package game

import game.entity.*
import game.entity.EntityType.*
import game.event.*
import game.system.*
import game.system.event.GameSystemEvent.GameSystemEvent
import map.Dungeon
import util.LineOfSight

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
    DeathHandlerSystem,
    SpawnEntitySystem,
    ExperienceSystem,
    EnemyAISystem,
    MovementSystem,
    VelocitySystem,
    WaveSystem,
    ItemUseSystem, // New unified item system
    HealingSystem, // Handles healing events
    ProjectileCreationSystem, // Handles projectile creation events
    MessageSystem, // Handles message events
    WaitSystem,
    OpenDoorSystem,
    CollisionCheckSystem,
    AttackSystem,
    RangeCheckSystem,
    CollisionHandlerSystem,
    DamageSystem,
    EquipInputSystem,
    EquipmentSystem,
    InventorySystem,
    InitiativeSystem,
    LevelUpSystem,
    SightMemorySystem,
  )

  def updateWithSystems(events: Seq[GameSystemEvent]): GameState = {
    @scala.annotation.tailrec
    def processEvents(currentState: GameState, eventsToProcess: Seq[GameSystemEvent]): GameState = {
      if (eventsToProcess.isEmpty) {
        currentState
      } else {
        val (newState, newEvents) = systems.foldLeft((currentState, eventsToProcess)) {
          case ((state, events), system) =>
            val (updatedState, generatedEvents) = system.update(state, events)
            (updatedState, generatedEvents)
        }
        
        // Process any newly generated events in the next iteration
        processEvents(newState, newEvents)
      }
    }
    
    processEvents(this, events)
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
      .filter(entity => entity.get[EntityTypeComponent].exists(c => 
        c.entityType == EntityType.Enemy || c.entityType == EntityType.Player || c.entityType.isInstanceOf[LockedDoor]
      ))
      .flatMap(_.get[Movement].map(_.position))
      .toSet
  
  lazy val drawableChanges: Seq[Set[(Point, Sprite)]] = {
    import game.entity.Drawable.*
    entities.map(_.sprites)
  }
}
