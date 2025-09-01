package game.entity

/**
 * Defines the effects an item can have when used.
 * Replaces hardcoded effect logic in the old item system.
 */
enum ItemEffect {
  /** Heals the target for a specific amount */
  case Heal(amount: Int)
  
  /** Creates a projectile with damage and optional explosion on death */
  case CreateProjectile(collisionDamage: Int, onDeathExplosion: Option[ExplosionEffect] = None)
}

/**
 * Defines explosion effects for projectiles.
 * Used by scrolls that create projectiles with area damage on impact.
 */
case class ExplosionEffect(radius: Int, damage: Int)