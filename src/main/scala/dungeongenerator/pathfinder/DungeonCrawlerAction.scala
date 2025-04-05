package dungeongenerator.pathfinder

sealed trait DungeonCrawlerAction

object DungeonCrawlerAction {
  case object Start extends DungeonCrawlerAction
  case object Moved extends DungeonCrawlerAction
  case object UnlockedDoor extends DungeonCrawlerAction
  case object PickedUpKey extends DungeonCrawlerAction
  case object PickedUpBossKey extends DungeonCrawlerAction
  case object ActivatedSwitch extends DungeonCrawlerAction
  case object Teleported extends DungeonCrawlerAction
}
