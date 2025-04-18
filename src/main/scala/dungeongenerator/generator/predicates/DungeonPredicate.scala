package dungeongenerator.generator.predicates

import dungeongenerator.generator.Entity.{Key, Switch, Teleporter, Treasure}
import dungeongenerator.generator.{Dungeon, Entity}

import scala.reflect.ClassTag

abstract class DungeonPredicate(dungeonScorePredicate: Dungeon => Either[String, Double]) {
  def dungeonScore(dungeon: Dungeon): Either[String, Double] =
    dungeonScorePredicate(dungeon).map(dungeonScore =>
      Math.min(dungeonScore, 1)
    )
}

case class RoomCountPredicate(targetRoomCount: Int)
  extends DungeonPredicate(dungeon =>
    if (dungeon.roomCount == targetRoomCount) Right(1.0)
    else if (dungeon.roomCount < targetRoomCount) Right(dungeon.roomCount / targetRoomCount.toDouble)
    else Left(s"Too many rooms - wanted $targetRoomCount but got ${dungeon.roomCount}")
  )

case class LongestRoomPathPredicate(targetPathLength: Int)
  extends DungeonPredicate(dungeon =>
    Right(dungeon.longestRoomPath.size / targetPathLength.toDouble)
  ) //TODO - find a way for this to use dungeon config

class EntityCount[T <: Entity](targetKeyAmount: Int)(implicit m: ClassTag[T]) extends DungeonPredicate(
  dungeon => {
    val entityCount = dungeon.count[T]

    if (entityCount > targetKeyAmount) Left(s"Too many entities - wanted $targetKeyAmount but got $entityCount")
    else Right(entityCount / targetKeyAmount.toDouble)
  }
)

case class KeyCountPredicate(targetAmount: Int) extends EntityCount[Key](targetAmount)

case class SwitchCountPredicate(targetAmount: Int) extends EntityCount[Switch](targetAmount)

case class TeleporterCountPredicate(targetAmount: Int) extends EntityCount[Teleporter](targetAmount)

case class NonPathRoomPredicate(targetAmount: Int) extends DungeonPredicate(dungeon =>
  Right(dungeon.roomLocations.filterNot(roomLocation => dungeon.longestRoomPath.exists(_._1.location == roomLocation)).size / targetAmount)
)

case class TreasurePredicate(targetAmount: Int) extends EntityCount[Treasure.type](targetAmount)
