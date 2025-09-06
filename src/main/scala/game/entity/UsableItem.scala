package game.entity

import game.entity.Ammo.AmmoType
import game.system.event.GameSystemEvent
import game.entity.Movement.position // Import for position extension method
import game.Point

/**
 * Universal usable item component that replaces type-specific item components.
 * This data-driven approach makes it easier to add new items without code changes.
 * Now uses type-safe functions with proper targeting context.
 * 
 * @param targeting How the item is targeted when used
 * @param effects Function that takes appropriate parameters and produces GameSystemEvents
 * @param consumeOnUse Whether the item is consumed when used (true for potions/scrolls, false for bows)
 * @param ammo Optional ammo type required to use this item (e.g., "Arrow" for bows)
 */
sealed trait UsableItem extends Component {
  def targeting: Targeting
  def consumeOnUse: Boolean
  def ammo: Option[AmmoType]
}

/**
 * Self-targeting item that affects the user directly (e.g., healing potions)
 */
case class SelfTargetingItem(
  effects: Entity => Seq[GameSystemEvent.GameSystemEvent], // Just needs the user
  consumeOnUse: Boolean = true,
  ammo: Option[AmmoType] = None
) extends UsableItem {
  override def targeting: Targeting = Targeting.Self
  
  // Custom equality that ignores the function object
  override def equals(other: Any): Boolean = other match {
    case that: SelfTargetingItem =>
      this.consumeOnUse == that.consumeOnUse &&
      this.ammo == that.ammo &&
      this.targeting == that.targeting
    case _ => false
  }
  
  override def hashCode(): Int = {
    val state = Seq(consumeOnUse, ammo, targeting)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

/**
 * Entity-targeting item that affects another entity (e.g., bows shooting enemies)
 */
case class EntityTargetingItem(
  effects: (Entity, Entity) => Seq[GameSystemEvent.GameSystemEvent], // (user, target entity)
  consumeOnUse: Boolean = true,
  ammo: Option[AmmoType] = None
) extends UsableItem {
  override def targeting: Targeting = Targeting.EnemyActor
  
  // Custom equality that ignores the function object
  override def equals(other: Any): Boolean = other match {
    case that: EntityTargetingItem =>
      this.consumeOnUse == that.consumeOnUse &&
      this.ammo == that.ammo &&
      this.targeting == that.targeting
    case _ => false
  }
  
  override def hashCode(): Int = {
    val state = Seq(consumeOnUse, ammo, targeting)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

/**
 * Tile-targeting item that affects a specific location (e.g., fireball scrolls)
 */
case class TileTargetingItem(
  range: Int,
  effects: (Entity, Point) => Seq[GameSystemEvent.GameSystemEvent], // (user, target point)
  consumeOnUse: Boolean = true,
  ammo: Option[AmmoType] = None
) extends UsableItem {
  override def targeting: Targeting = Targeting.TileInRange(range)
  
  // Custom equality that ignores the function object
  override def equals(other: Any): Boolean = other match {
    case that: TileTargetingItem =>
      this.range == that.range &&
      this.consumeOnUse == that.consumeOnUse &&
      this.ammo == that.ammo &&
      this.targeting == that.targeting
    case _ => false
  }
  
  override def hashCode(): Int = {
    val state = Seq(range, consumeOnUse, ammo, targeting)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object UsableItem {
  // Helper methods for working with usable items
  def isUsableItemEntity(entity: Entity): Boolean = {
    entity.components.values.exists(_.isInstanceOf[UsableItem])
  }
  
  def getUsableItem(entity: Entity): Option[UsableItem] = {
    entity.components.values.collectFirst {
      case usable: UsableItem => usable
    }
  }
  
  extension (entity: Entity) {
    def usableItem: Option[UsableItem] = getUsableItem(entity)
    def isUsableItem: Boolean = UsableItem.isUsableItemEntity(entity)
  }
  
  // Helper constructors for common item types using type-safe functions
  object builders {
    /** Create a self-targeting healing potion */
    def healingPotion(healAmount: Int = 40): SelfTargetingItem =
      SelfTargetingItem(
        effects = (user: Entity) => Seq(GameSystemEvent.HealEvent(user.id, healAmount)),
        consumeOnUse = true
      )
    
    /** Create an enemy-targeting bow that requires arrows */
    def bow(damage: Int = 8): EntityTargetingItem =
      EntityTargetingItem(
        effects = (user: Entity, target: Entity) => Seq(
          GameSystemEvent.CreateProjectileEvent(user.id, target.position, Some(target.id), damage)
        ),
        consumeOnUse = false, 
        ammo = Some(AmmoType.Arrow)
      )
    
    /** Create a tile-targeting fireball scroll with explosion */
    def fireballScroll(explosionRadius: Int = 2, explosionDamage: Int = 30): TileTargetingItem =
      TileTargetingItem(
        range = 10,
        effects = (user: Entity, targetPoint: Point) => Seq(
          GameSystemEvent.CreateProjectileEvent(user.id, targetPoint, None, 0, Some(ExplosionEffect(explosionRadius, explosionDamage)))
        ),
        consumeOnUse = true
      )
  }
}