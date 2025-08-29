package game.entity

import data.Sprites
import game.{Point, Sprite}

// Helper object to create various item entities
object ItemFactory {
  
  def createPotion(id: String): Entity = Entity(
    id = id,
    PotionItem(),
    CanPickUp(),
    Hitbox()
  )
  
  def createScroll(id: String): Entity = Entity(
    id = id,
    ScrollItem(),
    CanPickUp(),
    Hitbox()
  )
  
  def createArrow(id: String): Entity = Entity(
    id = id,
    ArrowItem(),
    CanPickUp(),
    Hitbox()
  )
  
  def createBow(id: String): Entity = Entity(
    id = id,
    BowItem(),
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