package game.entity

import game.entity.Ammo.AmmoType

/**
 * Component marking that an entity is ammunition with a specific tag.
 * Used for arrows, bolts, or other consumable ammo items.
 */
case class Ammo(ammoType: AmmoType) extends Component

object Ammo {
  enum AmmoType:
    case Arrow
    // Future ammo types can be added here, e.g., Bolt, Bullet, etc.
  
  extension (entity: Entity) {
    def isAmmo: Boolean = entity.has[Ammo]
  }
}