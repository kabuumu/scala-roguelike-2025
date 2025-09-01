package game.entity

import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{HealEvent, CreateProjectileEvent}

/**
 * Universal usable item component that replaces type-specific item components.
 * This data-driven approach makes it easier to add new items without code changes.
 * Now uses GameSystemEvents directly for better integration with the game system architecture.
 * 
 * @param targeting How the item is targeted when used
 * @param effects The effects the item has when activated (now using GameSystemEvent)
 * @param consumeOnUse Whether the item is consumed when used (true for potions/scrolls, false for bows)
 * @param ammo Optional ammo type required to use this item (e.g., "Arrow" for bows)
 */
case class UsableItem(
  targeting: Targeting,
  effects: Seq[GameSystemEvent.GameSystemEvent],
  consumeOnUse: Boolean = true,
  ammo: Option[String] = None
) extends Component

object UsableItem {
  extension (entity: Entity) {
    def usableItem: Option[UsableItem] = entity.get[UsableItem]
    def isUsableItem: Boolean = entity.has[UsableItem]
  }
  
  // Helper constructors for common item types using GameSystemEvents directly
  object builders {
    /** Create a self-targeting healing potion */
    def healingPotion(healAmount: Int = 40): UsableItem =
      UsableItem(Targeting.Self, Seq(HealEvent("", healAmount)), consumeOnUse = true)
    
    /** Create an enemy-targeting bow that requires arrows */
    def bow(damage: Int = 8, ammoType: String = "Arrow"): UsableItem =
      UsableItem(Targeting.EnemyActor, Seq(CreateProjectileEvent("", game.Point(0, 0), None, damage)), consumeOnUse = false, ammo = Some(ammoType))
    
    /** Create a tile-targeting fireball scroll with explosion */
    def fireballScroll(explosionRadius: Int = 2, explosionDamage: Int = 30): UsableItem =
      UsableItem(
        Targeting.TileInRange(10), // Reasonable range for targeting
        Seq(CreateProjectileEvent("", game.Point(0, 0), None, 0, Some(ExplosionEffect(explosionRadius, explosionDamage)))),
        consumeOnUse = true
      )
  }
}