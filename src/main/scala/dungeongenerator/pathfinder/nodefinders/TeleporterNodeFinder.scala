package dungeongenerator.pathfinder.nodefinders

import dungeongenerator.generator.Dungeon
import dungeongenerator.generator.Entity.Teleporter
import dungeongenerator.pathfinder.{DungeonCrawler, Node}
import dungeongenerator.pathfinder.DungeonCrawlerAction._

object TeleporterNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(currentCrawler@DungeonCrawler(currentPoint, _, _), Dungeon(dungeonEntities)) = currentNode

    for {
      (_, Teleporter(teleporterLocation)) <- dungeonEntities.collect {
        case (teleporterPoint, teleporter: Teleporter) if teleporterPoint == currentPoint => teleporterPoint -> teleporter
      }
    } yield
      currentNode.copy(
        currentCrawler = currentCrawler.copy(
          location = teleporterLocation,
          lastAction = Teleported
        )
      )
  }
}
