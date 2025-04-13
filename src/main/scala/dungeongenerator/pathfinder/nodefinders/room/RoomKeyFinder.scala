package dungeongenerator.pathfinder.nodefinders.room

import dungeongenerator.generator.Dungeon
import dungeongenerator.generator.Entity.{Key, Room}
import dungeongenerator.pathfinder.DungeonCrawlerAction.PickedUpKey
import dungeongenerator.pathfinder.nodefinders.NodeFinder
import dungeongenerator.pathfinder.{DungeonCrawler, Node}

object RoomKeyFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, currentInventory, _), currentDungeon@Dungeon(dungeonEntities)) = currentNode

    for {
      currentRoomPoint <- dungeonEntities.collectFirst {
        case (roomPoint, _: Room) if roomPoint == currentPoint => roomPoint
      }.toSeq
      keyEntity <- dungeonEntities.collect { case (keyPoint, key @ Key(_)) if keyPoint == currentRoomPoint => keyPoint -> key }
    } yield currentNode
      .updateCrawler(
        _.addItem(keyEntity._2)
          .addAction(PickedUpKey(keyEntity._2.colour))
      ).updateDungeon(
        _ - keyEntity
      )
  }
}
