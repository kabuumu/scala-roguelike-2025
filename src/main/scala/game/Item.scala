package game

object Item {
  val potionValue = 5

  sealed trait Item

  case object Potion extends Item

  case object Scroll extends Item

  case class Key(keyColour: KeyColour) extends Item

  case class Weapon(damage: Int, weaponType: WeaponType) extends Item {
    val range: Int = weaponType match {
      case Melee => 1
      case Ranged(range) => range
    }
  }

  sealed trait WeaponType
  case object Melee extends WeaponType
  case class Ranged(range: Int) extends WeaponType

  enum KeyColour {
    case Yellow, Red, Blue
  }
}
