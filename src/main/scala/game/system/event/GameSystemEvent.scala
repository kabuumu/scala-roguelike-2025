package game.system.event

import game.Direction
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
}

