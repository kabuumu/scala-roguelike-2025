package dungeongenerator.pathfinder.nodefinders

import dungeongenerator.generator.Entity._
import dungeongenerator.pathfinder.{DungeonCrawler, Node}
import dungeongenerator.pathfinder.DungeonCrawlerAction._

object DoorNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, inventory, _), currentDungeon) = currentNode

    val adjacentPoints = for {
      (newPoint, newPointType) <- currentDungeon.entities
      if currentPoint.adjacentPoints.toSet.contains(newPoint)
    } yield (newPoint, newPointType)

    adjacentPoints.collect {
      case lockedDoor@(newPoint, Door(Some(KeyLock))) if inventory.contains(Key) =>
        currentNode.copy(
          currentCrawler = currentNode.currentCrawler
            .removeFromInventory(Key)
            .copy(lastAction = UnlockedDoor),
          dungeonState = currentDungeon.copy(
            entities = currentDungeon.entities
              - lockedDoor
              + (newPoint -> Door(None))
          )
        )
      case lockedDoor@(newPoint, Door(Some(BossKeyLock))) if inventory.contains(BossKey) =>
        currentNode.copy(
          currentCrawler = currentNode.currentCrawler
            .removeFromInventory(BossKey)
            .copy(lastAction = UnlockedDoor),
          dungeonState = currentDungeon.copy(
            entities = currentDungeon.entities
              - lockedDoor
              + (newPoint -> Door(None))
          )
        )
      case (newPoint, Door(None)) =>
        currentNode.copy(
          currentCrawler = currentNode.currentCrawler.copy(
            location = newPoint,
            lastAction = Moved
          )
        )
    }


  }
}
