package map

import game.entity.*
import data.Sprites
import game.Sprite

// Simple descriptors for item types to replace the Item class in map generation
enum ItemDescriptor {
  case PotionDescriptor
  case ScrollDescriptor  
  case ArrowDescriptor
  case LeatherHelmetDescriptor
  case ChainmailArmorDescriptor
  case IronHelmetDescriptor
  case PlateArmorDescriptor
  case KeyDescriptor(keyColour: KeyColour)
  
  // Convert descriptor to actual entity
  def createEntity(id: String): Entity = this match {
    case PotionDescriptor => ItemFactory.createPotion(id)
    case ScrollDescriptor => ItemFactory.createScroll(id)
    case ArrowDescriptor => ItemFactory.createArrow(id)
    case LeatherHelmetDescriptor => EquippableItems.LeatherHelmet.createEntity(id)
    case ChainmailArmorDescriptor => EquippableItems.ChainmailArmor.createEntity(id)
    case IronHelmetDescriptor => EquippableItems.IronHelmet.createEntity(id)
    case PlateArmorDescriptor => EquippableItems.PlateArmor.createEntity(id)
    case KeyDescriptor(keyColour) => ItemFactory.createKey(id, keyColour)
  }
}