package dungeongenerator.pathfinder.nodefinders.room

import dungeongenerator.generator.Dungeon
import dungeongenerator.generator.Entity.{BossKey, Room}
import dungeongenerator.pathfinder.DungeonCrawlerAction.PickedUpBossKey
import dungeongenerator.pathfinder.nodefinders.NodeFinder
import dungeongenerator.pathfinder.{DungeonCrawler, Node}

//TODO refactor into standard inventory action
object RoomBossKeyFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, currentInventory, _), currentDungeon@Dungeon(dungeonEntities)) = currentNode

    for {
      currentRoomPoint <- dungeonEntities.collectFirst {
        case (roomPoint, _: Room) if roomPoint == currentPoint => roomPoint
      }.toSeq
      keyEntity <- dungeonEntities.collect { case (keyPoint, BossKey) if keyPoint == currentRoomPoint => keyPoint -> BossKey }
    } yield currentNode.updateCrawler(
      _.addItem(BossKey)
        .addAction(PickedUpBossKey)
    ).updateDungeon(
      _.copy(
        entities = currentDungeon.entities - keyEntity
      )
    )
  }
}
