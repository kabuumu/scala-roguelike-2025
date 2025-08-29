package game.entity

// Equipment slots
enum EquipmentSlot {
  case Helmet, Armor
}

// Component marking that an entity can be equipped by players
case class Equippable(slot: EquipmentSlot, damageReduction: Int, itemName: String) extends Component

object Equippable {
  extension (entity: Entity) {
    def equippable: Option[Equippable] = entity.get[Equippable]
    def isEquippable: Boolean = entity.has[Equippable]
  }
}