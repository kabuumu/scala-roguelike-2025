package dungeongenerator.pathfinder.nodefinders.room

import dungeongenerator.generator.Dungeon
import dungeongenerator.generator.Entity.{Key, Room}
import dungeongenerator.pathfinder.DungeonCrawlerAction.PickedUpKey
import dungeongenerator.pathfinder.{DungeonCrawler, Node}
import dungeongenerator.pathfinder.nodefinders.NodeFinder

object RoomKeyFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, currentInventory, _), currentDungeon@Dungeon(dungeonEntities)) = currentNode

    for {
      currentRoomPoint <- dungeonEntities.collectFirst {
        case (roomPoint, _: Room) if roomPoint == currentPoint => roomPoint
      }.toIterable
      keyEntity <- dungeonEntities.collect { case (keyPoint, Key) if keyPoint == currentRoomPoint => keyPoint -> Key }
    } yield currentNode.copy(
      currentCrawler =
        currentNode.currentCrawler.copy(
          inventory = currentInventory :+ Key,
          lastAction = PickedUpKey
        ),
      currentDungeon.copy(
        entities = currentDungeon.entities - keyEntity
      )
    )
  }
}
