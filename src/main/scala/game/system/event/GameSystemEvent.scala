package game.system.event

import game.Direction
import game.entity.{Entity, ExplosionEffect}
import ui.InputAction

object GameSystemEvent {
  sealed trait GameSystemEvent

  case class MoveAction(
    entityId: String,
    direction: Direction
  ) extends GameSystemEvent

  case class CollisionEvent(
    entityId: String,
    collidedWith: CollisionTarget
  ) extends GameSystemEvent

  enum CollisionTarget {
    case Entity(entityId: String)
    case Wall
  }
  
  case class InputEvent(
    entityId: String,
    input: InputAction
  ) extends GameSystemEvent

  case class DamageEvent(
    targetId: String,
    attackerId: String,
    baseDamage: Int
  ) extends GameSystemEvent
  
  case class SpawnEntityEvent(
    newEntity: Entity
  ) extends GameSystemEvent
  
  case class SpawnEntityWithCollisionCheckEvent(
    entityTemplate: Entity,
    preferredPositions: Seq[game.Point]
  ) extends GameSystemEvent
  
  case class AddExperienceEvent(
    entityId: String,
    experience: Int
  ) extends GameSystemEvent
  
  case class EquipEvent(
    entityId: String
  ) extends GameSystemEvent
  
  case class HealEvent(
    entityId: String,
    healAmount: Int
  ) extends GameSystemEvent
  
  case class MessageEvent(
    message: String
  ) extends GameSystemEvent
  
  case class RemoveItemEntityEvent(
    playerId: String,
    itemEntityId: String
  ) extends GameSystemEvent
  
  case class ResetInitiativeEvent(
    entityId: String
  ) extends GameSystemEvent
}
