package dungeongenerator.pathfinder

import dungeongenerator.generator.Dungeon
import dungeongenerator.pathfinder.DungeonCrawlerAction.{PickedUpKey, UnlockedDoor}

case class Node(currentCrawler: DungeonCrawler, dungeonState: Dungeon) {
  def updateCrawler(f: DungeonCrawler => DungeonCrawler): Node = {
    copy(currentCrawler = f(currentCrawler))
  }

  def updateDungeon(f: Dungeon => Dungeon): Node = {
    copy(dungeonState = f(dungeonState))
  }
}
