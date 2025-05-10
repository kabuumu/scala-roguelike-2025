package game

import game.Item.TargetType.{Point, Self}

object Item {
  val potionValue = 5

  enum ItemEffect {
    case Usable(targetType: TargetType)
    case Unusable
  }

  enum TargetType:
    case Self, Point


  sealed trait Item {
    def itemEffect: ItemEffect
  }

  case object Potion extends Item {
    val itemEffect: ItemEffect = ItemEffect.Usable(Self)
  }

  case object Scroll extends Item {
    val itemEffect: ItemEffect = ItemEffect.Usable(Point)
  }

  case class Key(keyColour: KeyColour) extends Item {
    val itemEffect: ItemEffect = ItemEffect.Unusable
  }

  case class Weapon(damage: Int, weaponType: WeaponType) extends Item {
    val range: Int = weaponType match {
      case Melee => 1
      case Ranged(range) => range
    }

    val itemEffect: ItemEffect = ItemEffect.Unusable
  }

  sealed trait WeaponType
  case object Melee extends WeaponType
  case class Ranged(range: Int) extends WeaponType

  enum KeyColour {
    case Yellow, Red, Blue
  }
}
