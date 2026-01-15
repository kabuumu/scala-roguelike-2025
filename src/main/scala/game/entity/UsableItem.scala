package game.entity

import data.Entities.EntityReference
import data.Projectiles.ProjectileReference
import game.entity.Ammo.AmmoType
import game.system.event.GameSystemEvent
import game.entity.Movement.position
import game.Point

/** Universal usable item component that replaces type-specific item components.
  * This data-driven approach makes it easier to add new items without code
  * changes. Now uses type-safe functions with proper targeting context.
  *
  * @param targeting
  *   How the item is targeted when used
  * @param effects
  *   Function that takes appropriate parameters and produces GameSystemEvents
  * @param consumeOnUse
  *   Whether the item is consumed when used (true for potions/scrolls, false
  *   for bows)
  * @param ammo
  *   Optional ammo type required to use this item (e.g., "Arrow" for bows)
  */
case class UsableItem(
    targeting: Targeting,
    chargeType: ChargeType,
    effect: GameEffect
) extends Component

enum ChargeType {
  case SingleUse
  case Ammo(ammoType: AmmoType)
  case InfiniteUse // For abilities like snake spit that can be used repeatedly
}

case class UseContext[T](userId: String, target: Option[T])

enum GameEffect {
  case Heal(amount: Int)
  case CreateProjectile(projectileReference: ProjectileReference)
  case ChainLightning(damage: Int, bounces: Int, bounceRange: Int)
}
