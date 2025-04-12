package dungeongenerator.pathfinder.nodefinders

import dungeongenerator.generator.Entity.Switch
import dungeongenerator.pathfinder.DungeonCrawlerAction.*
import dungeongenerator.pathfinder.{DungeonCrawler, Node}

object SwitchNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(currentCrawler@DungeonCrawler(currentPoint, _, _), currentDungeon) = currentNode

    currentDungeon.entities.collectFirst {
      case switchEntity@(point, Switch(switchAction)) if point == currentPoint =>
        currentNode.updateCrawler(
          _.addAction(ActivatedSwitch)
            .setLocation(point)
        ).updateDungeon(
          _.copy(
            entities = currentDungeon.entities - switchEntity
          )
        )
    }
  }
}
