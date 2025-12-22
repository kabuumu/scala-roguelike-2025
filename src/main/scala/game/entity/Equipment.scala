package game.entity

case class EquippedItem(id: String, stats: Equippable)

case class Equipment(
    helmet: Option[EquippedItem] = None,
    armor: Option[EquippedItem] = None,
    boots: Option[EquippedItem] = None,
    gloves: Option[EquippedItem] = None,
    weapon: Option[EquippedItem] = None
) extends Component {

  def equip(id: String, item: Equippable): (Equipment, Option[EquippedItem]) = {
    val equipped = EquippedItem(id, item)
    item.slot match {
      case EquipmentSlot.Helmet =>
        val previousItem = helmet
        (copy(helmet = Some(equipped)), previousItem)
      case EquipmentSlot.Armor =>
        val previousItem = armor
        (copy(armor = Some(equipped)), previousItem)
      case EquipmentSlot.Boots =>
        val previousItem = boots
        (copy(boots = Some(equipped)), previousItem)
      case EquipmentSlot.Gloves =>
        val previousItem = gloves
        (copy(gloves = Some(equipped)), previousItem)
      case EquipmentSlot.Weapon =>
        val previousItem = weapon
        (copy(weapon = Some(equipped)), previousItem)
    }
  }

  def unequip(slot: EquipmentSlot): Equipment = {
    slot match {
      case EquipmentSlot.Helmet => copy(helmet = None)
      case EquipmentSlot.Armor  => copy(armor = None)
      case EquipmentSlot.Boots  => copy(boots = None)
      case EquipmentSlot.Gloves => copy(gloves = None)
      case EquipmentSlot.Weapon => copy(weapon = None)
    }
  }

  def getEquippedItem(slot: EquipmentSlot): Option[EquippedItem] = {
    slot match {
      case EquipmentSlot.Helmet => helmet
      case EquipmentSlot.Armor  => armor
      case EquipmentSlot.Boots  => boots
      case EquipmentSlot.Gloves => gloves
      case EquipmentSlot.Weapon => weapon
    }
  }

  def getAllEquipped: Seq[EquippedItem] = {
    Seq(helmet, armor, boots, gloves, weapon).flatten
  }

  def getTotalDamageReduction: Int = {
    getAllEquipped.map(_.stats.damageReduction).sum
  }

  def getTotalDamageBonus: Int = {
    getAllEquipped.map(_.stats.damageBonus).sum
  }
}

object Equipment {
  extension (entity: Entity) {
    def equipment: Equipment = entity.get[Equipment].getOrElse(Equipment())

    def equipItemComponent(
        id: String,
        item: Equippable
    ): (Entity, Option[EquippedItem]) = {
      val currentEquipment = entity.equipment
      val (newEquipment, previousItem) = currentEquipment.equip(id, item)
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
