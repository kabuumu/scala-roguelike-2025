package game.entity

case class Equipment(
  helmet: Option[Equippable] = None,
  armor: Option[Equippable] = None
) extends Component {
  
  def equip(item: Equippable): (Equipment, Option[Equippable]) = {
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
  
  def getEquippedItem(slot: EquipmentSlot): Option[Equippable] = {
    slot match {
      case EquipmentSlot.Helmet => helmet
      case EquipmentSlot.Armor => armor
    }
  }
  
  def getAllEquipped: Seq[Equippable] = {
    Seq(helmet, armor).flatten
  }
  
  def getTotalDamageReduction: Int = {
    getAllEquipped.map(_.damageReduction).sum
  }
}

object Equipment {
  extension (entity: Entity) {
    def equipment: Equipment = entity.get[Equipment].getOrElse(Equipment())
    
    def equipItemComponent(item: Equippable): (Entity, Option[Equippable]) = {
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
