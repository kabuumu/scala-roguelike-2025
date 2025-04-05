package dungeongenerator.pathfinder.nodefinders.room

import dungeongenerator.generator.Dungeon
import dungeongenerator.generator.Entity.{BossKey, Room}
import dungeongenerator.pathfinder.DungeonCrawlerAction.PickedUpBossKey
import dungeongenerator.pathfinder.{DungeonCrawler, Node}
import dungeongenerator.pathfinder.nodefinders.NodeFinder

//TODO refactor into standard inventory action
object RoomBossKeyFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, currentInventory, _), currentDungeon@Dungeon(dungeonEntities)) = currentNode

    for {
      currentRoomPoint <- dungeonEntities.collectFirst {
        case (roomPoint, _: Room) if roomPoint == currentPoint => roomPoint
      }.toIterable
      keyEntity <- dungeonEntities.collect { case (keyPoint, BossKey) if keyPoint == currentRoomPoint => keyPoint -> BossKey }
    } yield currentNode.copy(
      currentCrawler =
        currentNode.currentCrawler.copy(
          inventory = currentInventory :+ BossKey,
          lastAction = PickedUpBossKey
        ),
      currentDungeon.copy(
        entities = currentDungeon.entities - keyEntity
      )
    )
  }
}
