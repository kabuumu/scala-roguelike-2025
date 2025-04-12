package dungeongenerator.pathfinder.nodefinders

import dungeongenerator.generator.Dungeon
import dungeongenerator.generator.Entity.Teleporter
import dungeongenerator.pathfinder.DungeonCrawlerAction.*
import dungeongenerator.pathfinder.{DungeonCrawler, Node}

object TeleporterNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(currentCrawler@DungeonCrawler(currentPoint, _, _), Dungeon(dungeonEntities)) = currentNode

    for {
      (_, Teleporter(teleporterLocation)) <- dungeonEntities.collect {
        case (teleporterPoint, teleporter: Teleporter) if teleporterPoint == currentPoint => teleporterPoint -> teleporter
      }
    } yield
      currentNode.updateCrawler(
        _.setLocation(teleporterLocation)
          .addAction(Teleported)
      )
  }
}
