package game.entity

import game.Point

enum CaravanState:
  case Idle
  case Moving
  case Trading

/** Component to manage a caravan of traders and followers.
  *
  * @param leaderId
  *   The entity ID of the caravan leader
  * @param members
  *   List of entity IDs that belong to this caravan (including leader and pack
  *   animals)
  * @param targetVillageCenter
  *   The center point of the target village
  * @param state
  *   Current state of the caravan
  * @param waitTimer
  *   Timer for waiting at trading stops
  */
case class CaravanComponent(
    leaderId: String,
    members: Seq[String],
    targetVillageCenter: Option[Point] = None,
    state: CaravanState = CaravanState.Idle,
    waitTimer: Int = 0
) extends Component

object CaravanComponent {
  // Wait for 10 seconds at a village (approx 600 ticks)
  val TradingDuration = 600
}
