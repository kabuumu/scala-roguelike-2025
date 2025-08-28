package game.entity

import game.Item.{EquippableItem, EquipmentSlot}

// Component marking that an entity can be equipped by players
case class Equippable(slot: EquipmentSlot, damageReduction: Int, itemName: String) extends Component

object Equippable {
  extension (entity: Entity) {
    def equippable: Option[Equippable] = entity.get[Equippable]
    def isEquippable: Boolean = entity.has[Equippable]
  }
  
  // Create an Equippable component from an EquippableItem
  def fromEquippableItem(item: EquippableItem): Equippable = 
    Equippable(item.slot, item.damageReduction, item.name)
}