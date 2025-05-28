package game.action

import game.GameState
import game.entity.Entity
import game.event.{ApplyPerkEvent, Event, LevelUpEvent, MessageEvent}
import game.perk.Perk

case class LevelUpAction(chosenPerk: Perk) extends Action {

  override def apply(entity: Entity, gameState: GameState): Seq[Event] = Seq(
    ApplyPerkEvent(entity.id, chosenPerk),
    LevelUpEvent(entity.id),
    MessageEvent(s"${entity.id} has leveled up!"),
  )
}
