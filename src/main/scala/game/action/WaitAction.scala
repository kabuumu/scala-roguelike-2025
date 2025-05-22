package game.action

import game.*
import game.entity.*
import game.entity.Initiative.*
import game.event.{Event, ResetInitiativeEvent}

case object WaitAction extends Action {
  def apply(entity: Entity, gameState: GameState): Seq[Event] = {
    Seq(
      ResetInitiativeEvent(entity.id)
    )
  }
}
