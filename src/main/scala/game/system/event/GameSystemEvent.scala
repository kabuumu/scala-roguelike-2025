package game.system.event

import game.Direction

object GameSystemEvent {
  sealed trait GameSystemEvent

  case class MoveAction(
    entityId: String,
    direction: Direction
  ) extends GameSystemEvent

}

