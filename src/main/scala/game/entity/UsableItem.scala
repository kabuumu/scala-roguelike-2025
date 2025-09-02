package game.entity

import game.entity.Ammo.AmmoType
import game.system.event.GameSystemEvent
import game.system.event.GameSystemEvent.{CreateProjectileEvent, HealEvent}
import game.entity.Movement.position // Import for position extension method

/**
 * Universal usable item component that replaces type-specific item components.
 * This data-driven approach makes it easier to add new items without code changes.
 * Now uses type-safe functions that receive the actual target instead of placeholder strings.
 * 
 * @param targeting How the item is targeted when used
 * @param effects Function that takes the target and produces GameSystemEvents
 * @param consumeOnUse Whether the item is consumed when used (true for potions/scrolls, false for bows)
 * @param ammo Optional ammo type required to use this item (e.g., "Arrow" for bows)
 */
case class UsableItem[T <: TargetType](
  targeting: Targeting[T],
  effects: (Entity, T) => Seq[GameSystemEvent.GameSystemEvent], // (user, target) => events
  consumeOnUse: Boolean = true,
  ammo: Option[AmmoType] = None
) extends Component

object UsableItem {
  // Type aliases for convenience
  type SelfTargeting = UsableItem[EntityTarget]
  type EnemyTargeting = UsableItem[EntityTarget]  
  type TileTargeting = UsableItem[TileTarget]
  
  // Helper methods for working with generic types
  def isUsableItemEntity(entity: Entity): Boolean = {
    entity.components.values.exists(_.isInstanceOf[UsableItem[?]])
  }
  
  def getUsableItem(entity: Entity): Option[UsableItem[?]] = {
    entity.components.values.collectFirst {
      case usable: UsableItem[?] => usable
    }
  }
  
  extension (entity: Entity) {
    def usableItem: Option[UsableItem[?]] = getUsableItem(entity)
    def isUsableItem: Boolean = UsableItem.isUsableItemEntity(entity)
  }
  
  // Helper constructors for common item types using type-safe functions
  object builders {
    /** Create a self-targeting healing potion */
    def healingPotion(healAmount: Int = 40): UsableItem[EntityTarget] =
      UsableItem(
        Targeting.Self, 
        (user: Entity, target: EntityTarget) => Seq(HealEvent(user.id, healAmount)),
        consumeOnUse = true
      )
    
    /** Create an enemy-targeting bow that requires arrows */
    def bow(damage: Int = 8): UsableItem[EntityTarget] =
      UsableItem(
        Targeting.EnemyActor, 
        (user: Entity, target: EntityTarget) => Seq(
          CreateProjectileEvent(user.id, target.entity.position, Some(target.entity.id), damage)
        ),
        consumeOnUse = false, 
        ammo = Some(AmmoType.Arrow)
      )
    
    /** Create a tile-targeting fireball scroll with explosion */
    def fireballScroll(explosionRadius: Int = 2, explosionDamage: Int = 30): UsableItem[TileTarget] =
      UsableItem(
        Targeting.TileInRange(10),
        (user: Entity, target: TileTarget) => Seq(
          CreateProjectileEvent(user.id, target.point, None, 0, Some(ExplosionEffect(explosionRadius, explosionDamage)))
        ),
        consumeOnUse = true
      )
  }
}