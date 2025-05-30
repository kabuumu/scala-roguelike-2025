package game.entity

import game.DeathDetails
import game.event.Event

case class DeathEvents(deathEvents: Seq[DeathDetails => Event] = Nil) extends Component {
  def add(event: DeathDetails => Event): DeathEvents = {
    DeathEvents(deathEvents :+ event)
  }
}
