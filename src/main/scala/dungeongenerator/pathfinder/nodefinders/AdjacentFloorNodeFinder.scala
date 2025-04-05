package dungeongenerator.pathfinder.nodefinders

import dungeongenerator.generator.Entity.Floor
import dungeongenerator.pathfinder.{DungeonCrawler, Node}
import dungeongenerator.pathfinder.DungeonCrawlerAction._

object AdjacentFloorNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, _, _), currentDungeon) = currentNode

    for {
      (newPoint, newPointType) <- currentDungeon.entities
      if currentPoint.adjacentPoints.toSet.contains(newPoint)
      if newPointType == Floor
    } yield currentNode.copy(
      currentCrawler = currentNode.currentCrawler.copy(
        location = newPoint,
        lastAction = Moved
      )
    ) //TODO: Add monocle to make this better
  }
}
