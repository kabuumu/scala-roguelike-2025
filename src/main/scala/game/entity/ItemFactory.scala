package game.entity

import data.Sprites
import game.entity.Ammo.AmmoType
import game.{Point, Sprite}
import map.ItemDescriptor

// Helper object to create various item entities using the new behavior-driven design
object ItemFactory {
  /** Create a healing potion using the new UsableItem component */
  def createPotion(id: String): Entity = Entity(
    id = id,
    UsableItem.builders.healingPotion(), // Self-targeted Heal(40), consumeOnUse=true
    CanPickUp(),
    Hitbox(),
    Drawable(Sprites.potionSprite) 
  )
  
  /** Create a fireball scroll using the new UsableItem component */
  def createScroll(id: String): Entity = Entity(
    id = id,
    UsableItem.builders.fireballScroll(), // TileInRange-targeted CreateProjectile with explosion
    CanPickUp(),
    Hitbox(),
    Drawable(Sprites.scrollSprite)
  )
  
  /** Create an arrow using the new Ammo component (non-usable) */
  def createArrow(id: String): Entity = Entity(
    id = id,
    Ammo(AmmoType.Arrow),
    CanPickUp(),
    Hitbox(),
    Drawable(Sprites.arrowSprite)
  )
  
  /** Create a bow using the new UsableItem component */
  def createBow(id: String): Entity = Entity(
    id = id,
    UsableItem.builders.bow(), // EnemyActor-targeted CreateProjectile(8), ammo="Arrow", consumeOnUse=false
    CanPickUp(),
    Hitbox(),
    Drawable(Sprites.bowSprite)
  )
  
  def createKey(id: String, keyColour: KeyColour): Entity = Entity(
    id = id,
    KeyItem(keyColour),
    CanPickUp(),
    Hitbox(),
    Drawable(keyColour match {
      case KeyColour.Yellow => Sprites.yellowKeySprite
      case KeyColour.Blue => Sprites.blueKeySprite
      case KeyColour.Red => Sprites.redKeySprite
    })
  )
  
  def createWeapon(id: String, damage: Int, weaponType: WeaponType): Entity = Entity(
    id = id,
    WeaponItem(damage, weaponType),
    CanPickUp(),
    Hitbox(),
    Drawable(weaponType match {
      case Melee => Sprites.defaultItemSprite //TODO - add sprite for weapons if needed
      case Ranged(_) => Sprites.bowSprite
    })
  )
  
  // Helper to add position and sprite to an item entity when placing in world
  def placeInWorld(entity: Entity, position: Point): Entity = {
    entity
      .addComponent(Movement(position = position))
  }
}