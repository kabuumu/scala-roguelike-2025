package dungeongenerator.pathfinder

import dungeongenerator.generator.Entity.KeyColour
import dungeongenerator.generator.Point

sealed trait DungeonCrawlerAction

object DungeonCrawlerAction {
  case object Start extends DungeonCrawlerAction
  case class Moved(from: Point, throughDoor: Point, to: Point) extends DungeonCrawlerAction
  case object UnlockedDoor extends DungeonCrawlerAction
  case class PickedUpKey(keyColour: KeyColour) extends DungeonCrawlerAction
  case object PickedUpBossKey extends DungeonCrawlerAction
  case object ActivatedSwitch extends DungeonCrawlerAction
  case object Teleported extends DungeonCrawlerAction
}
