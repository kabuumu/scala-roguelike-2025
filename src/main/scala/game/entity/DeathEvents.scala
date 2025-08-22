package game.entity

import game.DeathDetails
import game.system.event.GameSystemEvent.GameSystemEvent

case class DeathEvents(deathEvents: DeathDetails => Seq[GameSystemEvent] = _ => Nil) extends Component {
  def add(newDeathEvent: DeathDetails => GameSystemEvent): DeathEvents = {
    DeathEvents(details => deathEvents(details) :+ newDeathEvent(details))
  }
}
