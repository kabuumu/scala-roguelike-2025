package dungeongenerator.pathfinder.nodefinders

import dungeongenerator.generator.Entity.BossKey
import dungeongenerator.pathfinder.DungeonCrawlerAction.*
import dungeongenerator.pathfinder.{DungeonCrawler, Node}

object BossKeyNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, inventory, _), currentDungeon) = currentNode

    currentDungeon.entities.collectFirst {
      case bossKeyEntity@(point, BossKey) if point == currentPoint =>
        currentNode.updateCrawler(
          _.addItem(BossKey)
            .addAction(PickedUpBossKey)
        ).updateDungeon(
          _.copy(
            entities = currentDungeon.entities - bossKeyEntity
          )
        )
    }
  }
}
