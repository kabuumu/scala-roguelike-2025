package dungeongenerator.pathfinder.nodefinders

import dungeongenerator.generator.Entity.Floor
import dungeongenerator.pathfinder.DungeonCrawlerAction.*
import dungeongenerator.pathfinder.{DungeonCrawler, Node}

object AdjacentFloorNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, _, _), currentDungeon) = currentNode

    for {
      (newPoint, newPointType) <- currentDungeon.entities
      if currentPoint.adjacentPoints.toSet.contains(newPoint)
      if newPointType == Floor
    } yield currentNode.updateCrawler(
      _.setLocation(newPoint)
        .addAction(Moved)
    )
  }
}
