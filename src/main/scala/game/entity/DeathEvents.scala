package game.entity

import game.DeathDetails
import game.system.event.GameSystemEvent.GameSystemEvent
import data.DeathEvents.DeathEventReference

case class DeathEvents(deathEvents: Seq[DeathEventReference] = Nil) extends Component
