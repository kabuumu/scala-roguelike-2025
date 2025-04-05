package dungeongenerator.pathfinder.nodefinders.room

import dungeongenerator.generator.Dungeon
import dungeongenerator.generator.Entity.{Room, Switch}
import dungeongenerator.pathfinder.DungeonCrawlerAction.ActivatedSwitch
import dungeongenerator.pathfinder.{DungeonCrawler, Node}
import dungeongenerator.pathfinder.nodefinders.NodeFinder

object RoomSwitchFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(currentCrawler@DungeonCrawler(currentPoint, _, _), currentDungeon@Dungeon(dungeonEntities)) = currentNode

    for {
      currentRoomPoint <- dungeonEntities.collectFirst {
        case (roomPoint, _: Room) if roomPoint == currentPoint => roomPoint
      }.toIterable
      switchEntity@(_, Switch(switchAction)) <- dungeonEntities.collect { case (switchPoint, switch: Switch) if switchPoint == currentRoomPoint => switchPoint -> switch }
    } yield
      currentNode.copy(
        dungeonState = switchAction(currentDungeon) - switchEntity,
        currentCrawler = currentCrawler.copy(
          lastAction = ActivatedSwitch
        )
      )
  }
}
