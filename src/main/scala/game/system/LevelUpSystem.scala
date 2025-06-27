package game.system

import game.GameState
import game.entity.Experience.*
import game.system.event.GameSystemEvent
import ui.InputAction
import game.status.StatusEffect.*


object LevelUpSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent.GameSystemEvent]): (GameState, Seq[GameSystemEvent.GameSystemEvent]) = {
    val updatedGamestate = events.foldLeft(gameState) {
      case (currentState, event @ GameSystemEvent.InputEvent(entityId, InputAction.LevelUp(chosenPerk))) =>
        currentState.updateEntity(entityId, _.levelUp.addStatusEffect(chosenPerk))
      case (currentState, _) =>
        currentState
    }
    
    (updatedGamestate, Nil)
  }
  
}
