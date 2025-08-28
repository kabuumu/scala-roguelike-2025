package game.entity

import game.Item.*

case class Equipment(
  helmet: Option[EquippableItem] = None,
  armor: Option[EquippableItem] = None
) extends Component {
  
  def equip(item: EquippableItem): (Equipment, Option[EquippableItem]) = {
    item.slot match {
      case EquipmentSlot.Helmet => 
        val previousItem = helmet
        (copy(helmet = Some(item)), previousItem)
      case EquipmentSlot.Armor => 
        val previousItem = armor
        (copy(armor = Some(item)), previousItem)
    }
  }
  
  def unequip(slot: EquipmentSlot): Equipment = {
    slot match {
      case EquipmentSlot.Helmet => copy(helmet = None)
      case EquipmentSlot.Armor => copy(armor = None)
    }
  }
  
  def getEquippedItem(slot: EquipmentSlot): Option[EquippableItem] = {
    slot match {
      case EquipmentSlot.Helmet => helmet
      case EquipmentSlot.Armor => armor
    }
  }
  
  def getAllEquipped: Seq[EquippableItem] = {
    Seq(helmet, armor).flatten
  }
  
  def getTotalDamageReduction: Int = {
    getAllEquipped.map(_.damageReduction).sum
  }
}

object Equipment {
  extension (entity: Entity) {
    def equipment: Equipment = entity.get[Equipment].getOrElse(Equipment())
    
    def equipItem(item: EquippableItem): (Entity, Option[EquippableItem]) = {
      val currentEquipment = entity.equipment
      val (newEquipment, previousItem) = currentEquipment.equip(item)
      val updatedEntity = entity.update[Equipment](_ => newEquipment)
      (updatedEntity, previousItem)
    }
    
    def unequipItem(slot: EquipmentSlot): Entity = {
      entity.update[Equipment](_.unequip(slot))
    }
    
    def getTotalDamageReduction: Int = {
      entity.equipment.getTotalDamageReduction
    }
  }
}
