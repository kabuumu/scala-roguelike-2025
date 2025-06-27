package game.system

import game.GameState
import game.entity.Health.*
import game.system.event.GameSystemEvent.*
import game.system.event.GameSystemEvent

object DamageSystem extends GameSystem {
  override def update(gameState: GameState, events: Seq[GameSystemEvent]): (GameState, Seq[GameSystemEvent]) = {
    events.foldLeft(gameState) {
      case (currentState, GameSystemEvent.DamageEvent(entityId, attackerId, baseDamage)) =>
        currentState.updateEntity(entityId, _.damage(baseDamage, attackerId))
      case (currentState, _) =>
        currentState
    } -> Nil
  }

}
