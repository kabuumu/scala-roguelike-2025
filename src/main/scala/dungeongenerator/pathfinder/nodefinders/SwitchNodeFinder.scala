package dungeongenerator.pathfinder.nodefinders

import dungeongenerator.generator.Entity.Switch
import dungeongenerator.pathfinder.{DungeonCrawler, Node}
import dungeongenerator.pathfinder.DungeonCrawlerAction._

object SwitchNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(currentCrawler@DungeonCrawler(currentPoint, _, _), currentDungeon) = currentNode

    currentDungeon.entities.collectFirst {
      case switchEntity@(point, Switch(switchAction)) if point == currentPoint =>
        currentNode.copy(
          dungeonState =
            switchAction(currentDungeon) - switchEntity,
          currentCrawler = currentCrawler.copy(
            lastAction = ActivatedSwitch
          )
        )
    }
  }
}
