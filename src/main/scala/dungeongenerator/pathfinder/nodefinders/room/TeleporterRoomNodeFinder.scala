package dungeongenerator.pathfinder.nodefinders.room

import dungeongenerator.generator.Dungeon
import dungeongenerator.generator.Entity.{Room, Teleporter}
import dungeongenerator.pathfinder.DungeonCrawlerAction.Teleported
import dungeongenerator.pathfinder.{DungeonCrawler, Node}
import dungeongenerator.pathfinder.nodefinders.NodeFinder

object TeleporterRoomNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(currentCrawler@DungeonCrawler(currentPoint, _, _), Dungeon(dungeonEntities)) = currentNode

    for {
      currentRoomPoint <- dungeonEntities.collectFirst {
        case (roomPoint, _: Room) if roomPoint == currentPoint => roomPoint
      }.toIterable
      (_, Teleporter(teleporterLocation)) <- dungeonEntities.collect {
        case (teleporterPoint, teleporter: Teleporter) if teleporterPoint == currentRoomPoint => teleporterPoint -> teleporter
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
