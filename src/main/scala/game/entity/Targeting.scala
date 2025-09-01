package game.entity

import game.Point

/**
 * Defines how an item can be targeted when used.
 * Replaces hardcoded targeting logic in the old item system.
 */
enum Targeting {
  /** Item targets the user themselves (e.g., healing potions) */
  case Self
  
  /** Item targets an enemy actor entity (e.g., bow shooting at enemies) */
  case EnemyActor
  
  /** Item targets a tile within a specific range (e.g., fireball scrolls) */
  case TileInRange(range: Int)
}