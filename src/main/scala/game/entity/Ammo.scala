package game.entity

/**
 * Component marking that an entity is ammunition with a specific tag.
 * Used for arrows, bolts, or other consumable ammo items.
 */
case class Ammo(tag: String) extends Component

object Ammo {
  extension (entity: Entity) {
    def ammo: Option[Ammo] = entity.get[Ammo]
    def isAmmo: Boolean = entity.has[Ammo]
    def isAmmoType(tag: String): Boolean = entity.get[Ammo].exists(_.tag == tag)
  }
}