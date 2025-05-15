package game

import game.Item.TargetType.{Point, Self}

object Item {
  val potionValue = 5

  //Types of item
  // Unusable (e.g. key, melee weapon, armour or ammo - has uses but not by direct use)
  //    This is assuming that equipping is a separate action from using
  //    This is a bit of a misnomer, as you can use a key to unlock a door, but the use action is simply walking into the door
  //    Weapons could be considered as usable, but currently they are used by a separate attack action
  // Single use usable (e.g. potion or scroll)
  // Multi use usable (e.g. wand or staff) would need a charges mechanic
  // Usable with ammo (e.g. bow or gun) would need linking to an ammo item

  object ItemTypes {
    case class ItemCharge(currentCharge: Int, maxCharge: Int)

    sealed trait Item
    sealed trait UsableItem extends Item {
      def targetType: TargetType
    }
    case class SingleUseItem(targetType: TargetType) extends UsableItem
    case class MultiUseItem(targetType: TargetType, charges: ItemCharge) extends UsableItem
    case class LoadableItem(targetType: TargetType, ammoType: Item) extends UsableItem
    case class UnusableItem() extends Item
  }

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
