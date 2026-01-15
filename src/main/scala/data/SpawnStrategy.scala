package data

enum SpawnStrategy:
  case TargetNearestEnemy(range: Int)
  case TargetRandomEnemy(range: Int)
  case ExcludeKiller
  case ExcludeCreator
  case ExcludeSpecific(id: String)
