package game.entity

// Helper object to create equipment item entities
object EquippableItems {
  
  // Helmet items
  object LeatherHelmet {
    def createEntity(id: String): Entity = Entity(
      id = id,
      CanPickUp(),
      Equippable(EquipmentSlot.Helmet, 2, "Leather Helmet"),
      Hitbox()
    )
  }
  
  object IronHelmet {
    def createEntity(id: String): Entity = Entity(
      id = id,
      CanPickUp(),
      Equippable(EquipmentSlot.Helmet, 4, "Iron Helmet"),
      Hitbox()
    )
  }
  
  // Armor items
  object ChainmailArmor {
    def createEntity(id: String): Entity = Entity(
      id = id,
      CanPickUp(),
      Equippable(EquipmentSlot.Armor, 5, "Chainmail Armor"),
      Hitbox()
    )
  }
  
  object PlateArmor {
    def createEntity(id: String): Entity = Entity(
      id = id,
      CanPickUp(),
      Equippable(EquipmentSlot.Armor, 8, "Plate Armor"),
      Hitbox()
    )
  }
}