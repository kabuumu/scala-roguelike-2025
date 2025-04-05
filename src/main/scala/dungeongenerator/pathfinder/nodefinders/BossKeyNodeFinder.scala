package dungeongenerator.pathfinder.nodefinders

import dungeongenerator.generator.Entity.BossKey
import dungeongenerator.pathfinder.{DungeonCrawler, Node}
import dungeongenerator.pathfinder.DungeonCrawlerAction._

object BossKeyNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, inventory, _), currentDungeon) = currentNode

    currentDungeon.entities.collectFirst {
      case keyEntity@(point, BossKey) if point == currentPoint =>
        currentNode.copy(
          currentCrawler =
            currentNode.currentCrawler.copy(
              inventory = inventory :+ BossKey,
              lastAction = PickedUpBossKey
            ),
          currentDungeon.copy(
            entities = currentDungeon.entities - keyEntity
          )
        )
    }
  }
}
