package game.entity

/**
 * Defines explosion effects for projectiles.
 * Used by scrolls and other effects that create projectiles with area damage on impact.
 * Moved from ItemEffect to be reusable across the entire event system.
 */
case class ExplosionEffect(radius: Int, damage: Int)