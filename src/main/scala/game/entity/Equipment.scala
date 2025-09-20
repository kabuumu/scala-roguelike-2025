package game.entity

case class Equipment(
  helmet: Option[Equippable] = None,
  armor: Option[Equippable] = None,
  boots: Option[Equippable] = None,
  gloves: Option[Equippable] = None,
  weapon: Option[Equippable] = None
) extends Component {
  
  def equip(item: Equippable): (Equipment, Option[Equippable]) = {
    item.slot match {
      case EquipmentSlot.Helmet => 
        val previousItem = helmet
        (copy(helmet = Some(item)), previousItem)
      case EquipmentSlot.Armor => 
        val previousItem = armor
        (copy(armor = Some(item)), previousItem)
      case EquipmentSlot.Boots => 
        val previousItem = boots
        (copy(boots = Some(item)), previousItem)
      case EquipmentSlot.Gloves => 
        val previousItem = gloves
        (copy(gloves = Some(item)), previousItem)
      case EquipmentSlot.Weapon => 
        val previousItem = weapon
        (copy(weapon = Some(item)), previousItem)
    }
  }
  
  def unequip(slot: EquipmentSlot): Equipment = {
    slot match {
      case EquipmentSlot.Helmet => copy(helmet = None)
      case EquipmentSlot.Armor => copy(armor = None)
      case EquipmentSlot.Boots => copy(boots = None)
      case EquipmentSlot.Gloves => copy(gloves = None)
      case EquipmentSlot.Weapon => copy(weapon = None)
    }
  }
  
  def getEquippedItem(slot: EquipmentSlot): Option[Equippable] = {
    slot match {
      case EquipmentSlot.Helmet => helmet
      case EquipmentSlot.Armor => armor
      case EquipmentSlot.Boots => boots
      case EquipmentSlot.Gloves => gloves
      case EquipmentSlot.Weapon => weapon
    }
  }
  
  def getAllEquipped: Seq[Equippable] = {
    Seq(helmet, armor, boots, gloves, weapon).flatten
  }
  
  def getTotalDamageReduction: Int = {
    getAllEquipped.map(_.damageReduction).sum
  }
  
  def getTotalDamageBonus: Int = {
    getAllEquipped.map(_.damageBonus).sum
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
    
    def getTotalDamageBonus: Int = {
      entity.equipment.getTotalDamageBonus
    }
  }
}
