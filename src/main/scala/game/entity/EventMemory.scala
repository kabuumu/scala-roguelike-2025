package game.entity

case class EventMemory(events: Seq[MemoryEvent] = Seq.empty) extends Component {
  def addEvent(event: MemoryEvent): EventMemory = {
    copy(events = events :+ event)
  }
  
  def getEvents: Seq[MemoryEvent] = events
  
  def getEventsByType[T <: MemoryEvent](implicit classTag: scala.reflect.ClassTag[T]): Seq[T] = {
    events.collect { case event if classTag.runtimeClass.isInstance(event) => event.asInstanceOf[T] }
  }
}

sealed trait MemoryEvent {
  def timestamp: Long
}

object MemoryEvent {
  case class ItemUsed(
    timestamp: Long,
    itemType: String,
    target: Option[String] = None
  ) extends MemoryEvent

  case class DamageTaken(
    timestamp: Long,
    damage: Int,
    baseDamage: Int,
    modifier: Int,
    source: String
  ) extends MemoryEvent

  case class DamageDealt(
    timestamp: Long,
    damage: Int,
    baseDamage: Int,
    modifier: Int,
    target: String
  ) extends MemoryEvent

  case class EnemyDefeated(
    timestamp: Long,
    enemyType: String,
    method: String
  ) extends MemoryEvent

  case class MovementStep(
    timestamp: Long,
    direction: game.Direction,
    fromPosition: game.Point,
    toPosition: game.Point
  ) extends MemoryEvent
}

object EventMemory {
  extension (entity: Entity) {
    def addMemoryEvent(event: MemoryEvent): Entity = {
      entity.update[EventMemory](_.addEvent(event))
    }
    
    def getMemoryEvents: Seq[MemoryEvent] = {
      entity.get[EventMemory].map(_.getEvents).getOrElse(Seq.empty)
    }
    
    def getMemoryEventsByType[T <: MemoryEvent](implicit classTag: scala.reflect.ClassTag[T]): Seq[T] = {
      entity.get[EventMemory].map(_.getEventsByType[T]).getOrElse(Seq.empty)
    }
    
    def getStepCount: Int = {
      entity.getMemoryEventsByType[MemoryEvent.MovementStep].length
    }
  }
}