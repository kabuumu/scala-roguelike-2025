package game.entity

import game.Point

/**
 * Target types for type-safe item usage
 */
sealed trait TargetType
case class EntityTarget(entity: Entity) extends TargetType  
case class TileTarget(point: Point) extends TargetType

/**
 * Defines how an item can be targeted when used.
 * Now type-safe with target type information.
 */
enum Targeting[T <: TargetType] {
  /** Item targets the user themselves (e.g., healing potions) */
  case Self extends Targeting[EntityTarget]
  
  /** Item targets an enemy actor entity (e.g., bow shooting at enemies) */
  case EnemyActor extends Targeting[EntityTarget]
  
  /** Item targets a tile within a specific range (e.g., fireball scrolls) */
  case TileInRange(range: Int) extends Targeting[TileTarget]
}