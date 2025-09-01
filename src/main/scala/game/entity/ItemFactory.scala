package game.entity

import data.Sprites
import game.{Point, Sprite}

// Helper object to create various item entities using the new behavior-driven design
object ItemFactory {
  
  /** Create a healing potion using the new UsableItem component */
  def createPotion(id: String): Entity = Entity(
    id = id,
    UsableItem.builders.healingPotion(), // Self-targeted Heal(40), consumeOnUse=true
    CanPickUp(),
    Hitbox()
  )
  
  /** Create a fireball scroll using the new UsableItem component */
  def createScroll(id: String): Entity = Entity(
    id = id,
    UsableItem.builders.fireballScroll(), // TileInRange-targeted CreateProjectile with explosion
    CanPickUp(),
    Hitbox()
  )
  
  /** Create an arrow using the new Ammo component (non-usable) */
  def createArrow(id: String): Entity = Entity(
    id = id,
    Ammo("Arrow"), // Ammo type, not directly usable
    CanPickUp(),
    Hitbox()
  )
  
  /** Create a bow using the new UsableItem component */
  def createBow(id: String): Entity = Entity(
    id = id,
    UsableItem.builders.bow(), // EnemyActor-targeted CreateProjectile(8), ammo="Arrow", consumeOnUse=false
    CanPickUp(),
    Hitbox()
  )
  
  def createKey(id: String, keyColour: KeyColour): Entity = Entity(
    id = id,
    KeyItem(keyColour),
    CanPickUp(),
    Hitbox()
  )
  
  def createWeapon(id: String, damage: Int, weaponType: WeaponType): Entity = Entity(
    id = id,
    WeaponItem(damage, weaponType),
    CanPickUp(),
    Hitbox()
  )
  
  // Helper to add position and sprite to an item entity when placing in world
  def placeInWorld(entity: Entity, position: Point, sprite: Sprite): Entity = {
    entity
      .addComponent(Movement(position = position))
      .addComponent(Drawable(sprite))
  }
}