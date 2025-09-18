package map

import game.entity.*
import data.{Items, Sprites}
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
    case PotionDescriptor => Items.healingPotion(id)
    case ScrollDescriptor => Items.fireballScroll(id)
    case ArrowDescriptor => Items.arrow(id)
    case LeatherHelmetDescriptor => Items.leatherHelmet(id)
    case ChainmailArmorDescriptor => Items.chainmailArmor(id)
    case IronHelmetDescriptor => Items.ironHelmet(id)
    case PlateArmorDescriptor => Items.plateArmor(id)
    case KeyDescriptor(keyColour) => Items.key(id, keyColour)
  }
}