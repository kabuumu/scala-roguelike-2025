package dungeongenerator.pathfinder.nodefinders

import dungeongenerator.generator.Entity.Key
import dungeongenerator.pathfinder.{DungeonCrawler, Node}
import dungeongenerator.pathfinder.DungeonCrawlerAction._

object KeyNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, inventory, _), currentDungeon) = currentNode

    currentDungeon.entities.collectFirst {
      case keyEntity@(point, Key) if point == currentPoint =>
        currentNode.copy(
          currentCrawler =
            currentNode.currentCrawler.copy(
              inventory = inventory :+ Key,
              lastAction = PickedUpKey
            ),
          currentDungeon.copy(
            entities = currentDungeon.entities - keyEntity
          )
        )
    }
  }
}
