package dungeongenerator.pathfinder

import dungeongenerator.generator.{Entity, Point}
import dungeongenerator.pathfinder.DungeonCrawlerAction.Start

case class DungeonCrawler(location: Point, inventory: Seq[Entity] = Nil, actions: Seq[DungeonCrawlerAction] = Seq(Start)) {
  override def hashCode(): Int = {
    31 + location.hashCode() + inventory.hashCode() + lastAction.hashCode()
  }

  override def equals(obj: Any): Boolean = obj match {
    case dungeonCrawler: DungeonCrawler =>
      dungeonCrawler.location == location && dungeonCrawler.inventory == inventory && dungeonCrawler.lastAction == dungeonCrawler.lastAction
    case _ =>
      false
  }

  def removeItem(entity: Entity): DungeonCrawler = {
    copy(inventory = inventory.diff(Seq(entity)))
  }

  def addItem(entity: Entity): DungeonCrawler = {
    copy(inventory = inventory :+ entity)
  }

  def addAction(action: DungeonCrawlerAction): DungeonCrawler = {
    copy(actions = actions :+ action)
  }

  val lastAction: DungeonCrawlerAction = actions.last

  def setLocation(location: Point): DungeonCrawler = {
    copy(location = location)
  }
}
