package data

import data.Projectiles.ProjectileReference
import data.Projectiles.ProjectileReference.Fireball
import game.entity.*
import game.entity.Ammo.AmmoType
import game.entity.Ammo.AmmoType.Arrow
import game.entity.ChargeType.SingleUse
import game.entity.GameEffect.Heal
import game.entity.Targeting.{EnemyActor, Self, TileInRange}

object Items {
  enum ItemReference:
    case HealingPotion
    case FireballScroll
    case Arrow
    case Bow
    case YellowKey
    case BlueKey
    case RedKey
    case MeleeWeapon(damage: Int)
    case RangedWeapon(damage: Int, range: Int)
    case LeatherHelmet
    case ChainmailArmor
    case IronHelmet
    case PlateArmor

  def healingPotion(id: String): Entity = Entity(
    id = id,
    NameComponent("Healing Potion", "Restores 40 health points when consumed"),
    UsableItem(Self, SingleUse, Heal(40)),
    CanPickUp(),
    Hitbox(),
    Drawable(Sprites.potionSprite) 
  )
  
  def fireballScroll(id: String): Entity = Entity(
    id = id,
    NameComponent("Fireball Scroll", "Unleashes a fireball at target location with radius 2 explosion"),
    UsableItem(TileInRange(10), SingleUse, GameEffect.CreateProjectile(Fireball)), 
    CanPickUp(),
    Hitbox(),
    Drawable(Sprites.scrollSprite)
  )
  
  def arrow(id: String): Entity = Entity(
    id = id,
    NameComponent("Arrow", "Ammunition for bows and crossbows"),
    Ammo(AmmoType.Arrow),
    CanPickUp(),
    Hitbox(),
    Drawable(Sprites.arrowSprite)
  )
  
  def bow(id: String): Entity = Entity(
    id = id,
    NameComponent("Bow", "Ranged weapon that fires arrows at enemies"),
    UsableItem(EnemyActor(10), ChargeType.Ammo(Arrow), GameEffect.CreateProjectile(ProjectileReference.Arrow)),
    CanPickUp(),
    Hitbox(),
    Drawable(Sprites.bowSprite)
  )
  
  def key(id: String, keyColour: KeyColour): Entity = Entity(
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
  
  def weapon(id: String, damage: Int, weaponType: WeaponType): Entity = Entity(
    id = id,
    WeaponItem(damage, weaponType),
    CanPickUp(),
    Hitbox(),
    Drawable(weaponType match {
      case Melee => Sprites.defaultItemSprite //TODO - add sprite for weapons if needed
      case Ranged(_) => Sprites.bowSprite
    })
  )

  // Equipment items
  def leatherHelmet(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable(EquipmentSlot.Helmet, 2, "Leather Helmet"),
    Hitbox(),
    Drawable(Sprites.leatherHelmetSprite)
  )
  
  def ironHelmet(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable(EquipmentSlot.Helmet, 4, "Iron Helmet"),
    Hitbox(),
    Drawable(Sprites.ironHelmetSprite)
  )
  
  def chainmailArmor(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable(EquipmentSlot.Armor, 5, "Chainmail Armor"),
    Hitbox(),
    Drawable(Sprites.chainmailArmorSprite)
  )
  
  def plateArmor(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable(EquipmentSlot.Armor, 8, "Plate Armor"),
    Hitbox(),
    Drawable(Sprites.plateArmorSprite)
  )
}