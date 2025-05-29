package game.entity

import game.event.Event

case class DeathEvents(deathEvents: Seq[Event]) extends Component
