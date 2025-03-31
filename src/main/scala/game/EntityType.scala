package game

enum EntityType(val isStatic: Boolean, val blocksMovement: Boolean):
  case Player extends EntityType(false, true)
  case Enemy extends EntityType(false, true)
  case Wall extends EntityType(true, true)
  case Floor extends EntityType(true, false)
