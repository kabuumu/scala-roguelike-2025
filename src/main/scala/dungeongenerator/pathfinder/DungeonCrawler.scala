package dungeongenerator.pathfinder

import dungeongenerator.generator.{Entity, Point}
import dungeongenerator.pathfinder.DungeonCrawlerAction.Start

case class DungeonCrawler(location: Point, inventory: Seq[Entity] = Nil, lastAction: DungeonCrawlerAction = Start) {
  def removeFromInventory(entity: Entity): DungeonCrawler = {
    copy(inventory = inventory.diff(Seq(entity)))
  }
}
