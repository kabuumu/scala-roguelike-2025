package dungeongenerator.pathfinder.nodefinders

import dungeongenerator.generator.Entity.*
import dungeongenerator.pathfinder.DungeonCrawlerAction.*
import dungeongenerator.pathfinder.{DungeonCrawler, Node}

object DoorNodeFinder extends NodeFinder {
  override def getPossibleNodes(currentNode: Node): Iterable[Node] = {
    val Node(DungeonCrawler(currentPoint, inventory, _), currentDungeon) = currentNode

    val adjacentPoints = for {
      (newPoint, newPointType) <- currentDungeon.entities
      if currentPoint.adjacentPoints.toSet.contains(newPoint)
    } yield (newPoint, newPointType)

    adjacentPoints.collect {
      case lockedDoor@(newPoint, Door(Some(KeyLock))) if inventory.contains(Key) =>
        currentNode.updateCrawler(
            _.removeItem(Key)
              .addAction(UnlockedDoor)
          )
          .updateDungeon(
            _.copy(
              entities = currentDungeon.entities
                - lockedDoor
                + (newPoint -> Door(None))
            )
          )
      case lockedDoor@(newPoint, Door(Some(BossKeyLock))) if inventory.contains(BossKey) =>
        currentNode.updateCrawler(
          _.removeItem(BossKey)
            .addAction(UnlockedDoor)
        ).updateDungeon(
          _.copy(
            entities = currentDungeon.entities
              - lockedDoor
              + (newPoint -> Door(None))
          )
        )
      case (newPoint, Door(None)) =>
        currentNode.updateCrawler(
          _.setLocation(newPoint)
            .addAction(Moved)
        )
    }
  }
}
