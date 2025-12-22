package game.system

import game.GameState
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.*
import ui.InputAction

object EquipInputSystem extends GameSystem {
  override def update(
      gameState: GameState,
      events: Seq[GameSystemEvent]
  ): (GameState, Seq[GameSystemEvent]) = {
    val (newState, newEvents) =
      events.foldLeft((gameState, Seq.empty[GameSystemEvent])) {
        case (
              (currentState, currentEvents),
              InputEvent(entityId, InputAction.Equip)
            ) =>
          (currentState, currentEvents :+ EquipEvent(entityId))
        case (
              (currentState, currentEvents),
              InputEvent(entityId, InputAction.EquipSpecific(target))
            ) =>
          (currentState, currentEvents :+ EquipSpecificEvent(entityId, target))
        case (
              (currentState, currentEvents),
              InputEvent(entityId, InputAction.UnequipItem(slot))
            ) =>
          (currentState, currentEvents :+ UnequipEvent(entityId, slot))
        case ((currentState, currentEvents), _) =>
          (currentState, currentEvents)
      }
    (newState, newEvents)
  }
}
