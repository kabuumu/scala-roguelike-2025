package game.system

import game.GameState
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*
import ui.InputAction

object EquipInputSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    val (newState, newEvents) = events.foldLeft((gameState, Seq.empty[GameSystemEvent])) {
      case ((currentState, currentEvents), InputEvent(entityId, InputAction.Equip)) =>
        (currentState, currentEvents :+ EquipEvent(entityId))
      case ((currentState, currentEvents), _) =>
        (currentState, currentEvents)
    }
    (newState, newEvents)
  }
}
