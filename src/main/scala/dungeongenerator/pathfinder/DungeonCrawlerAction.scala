package dungeongenerator.pathfinder

import dungeongenerator.generator.Entity.Room
import dungeongenerator.generator.Point

sealed trait DungeonCrawlerAction

object DungeonCrawlerAction {
  case object Start extends DungeonCrawlerAction
  case class Moved(from: Point, throughDoor: Point, to: Point) extends DungeonCrawlerAction
  case object UnlockedDoor extends DungeonCrawlerAction
  case object PickedUpKey extends DungeonCrawlerAction
  case object PickedUpBossKey extends DungeonCrawlerAction
  case object ActivatedSwitch extends DungeonCrawlerAction
  case object Teleported extends DungeonCrawlerAction
}
