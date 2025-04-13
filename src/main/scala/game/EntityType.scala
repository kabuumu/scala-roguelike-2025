package game

import dungeongenerator.generator.Entity.KeyColour
import game.Item.Item

enum EntityType(val isStatic: Boolean, val blocksMovement: Boolean):
  case Player extends EntityType(false, true)
  case Enemy extends EntityType(false, true)
  case Wall extends EntityType(true, true)
  case Floor extends EntityType(true, false)
  case LockedDoor(keyColour: KeyColour) extends EntityType(true, true)
  case Key(keyColour: KeyColour) extends EntityType(true, false)
  case ItemEntity(itemType: Item) extends EntityType(true, false)
