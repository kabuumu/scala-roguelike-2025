package game

enum EntityType(val isStatic: Boolean):
  case Player extends EntityType(false)
  case Enemy extends EntityType(false)
  case Wall extends EntityType(true)
  case Floor extends EntityType(true)
