package dungeongenerator.generator

sealed trait Entity

object Entity {

  case object Wall extends Entity

  case object Floor extends Entity

  case class Door(optLock: Option[Lock]) extends Entity {
    val isLocked: Boolean = optLock.isDefined
    val isOpen: Boolean = optLock.isEmpty
  }

  sealed trait Lock

  case class ItemLock(item: Entity) extends Lock

  case object BossKeyLock extends Lock

  case object SwitchLock extends Lock

  case class Key(colour: KeyColour) extends Entity

  enum KeyColour {
    case Yellow, Red, Blue
  }

  case object BossKey extends Entity

  case object StartPoint extends Entity

  case object Target extends Entity

  case class Room(entities: Iterable[(Point, Entity)]) extends Entity

  case class Switch(switchAction: Dungeon => Dungeon) extends Entity

  case class Teleporter(target: Point) extends Entity

  case object BossRoom extends Entity

  case object Treasure extends Entity

  //Wrapper class idea
  case class Locked(lockedEntity: Entity) //remove lock on unlock/switch activation? Need to define type of lock
  case class Unpowered(unpoweredEntity: Entity) // Remove unpowered state on power - what if entity needs to be re-powered?

}
