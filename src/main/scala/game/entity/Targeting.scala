package game.entity

import game.Point
import game.system.event.GameSystemEvent

/**
 * Defines how an item can be targeted when used.
 * Each targeting type has its own function signature for type safety.
 */
enum Targeting {
  /** Item targets the user themselves (e.g., healing potions) - no additional target needed */
  case Self
  
  /** Item targets an enemy actor entity (e.g., bow shooting at enemies) */
  case EnemyActor(range: Int)
  
  /** Item targets a tile within a specific range (e.g., fireball scrolls) */
  case TileInRange(range: Int)
}