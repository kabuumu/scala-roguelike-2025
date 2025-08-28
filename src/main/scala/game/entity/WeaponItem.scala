package game.entity

// Weapon types
sealed trait WeaponType
case object Melee extends WeaponType
case class Ranged(range: Int) extends WeaponType

// Component marking that an entity is a weapon
case class WeaponItem(damage: Int, weaponType: WeaponType) extends Component {
  val range: Int = weaponType match {
    case Melee => 1
    case Ranged(range) => range
  }
}

object WeaponItem {
  extension (entity: Entity) {
    def weaponItem: Option[WeaponItem] = entity.get[WeaponItem]
    def isWeapon: Boolean = entity.has[WeaponItem]
  }
}