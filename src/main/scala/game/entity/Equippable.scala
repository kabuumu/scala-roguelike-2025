package game.entity

// Equipment slots
enum EquipmentSlot {
  case Helmet, Armor, Boots, Gloves, Weapon
}

// Component marking that an entity can be equipped by players
case class Equippable(slot: EquipmentSlot, damageReduction: Int, damageBonus: Int, itemName: String) extends Component

object Equippable {
  // Convenience constructor for armor items (damage reduction only)
  def armor(slot: EquipmentSlot, damageReduction: Int, itemName: String): Equippable =
    Equippable(slot, damageReduction, 0, itemName)
    
  // Convenience constructor for weapon items (damage bonus only)
  def weapon(damageBonus: Int, itemName: String): Equippable =
    Equippable(EquipmentSlot.Weapon, 0, damageBonus, itemName)

  extension (entity: Entity) {
    def equippable: Option[Equippable] = entity.get[Equippable]
    def isEquippable: Boolean = entity.has[Equippable]
  }
}