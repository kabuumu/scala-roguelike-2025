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
    case LeatherHelmet
    case ChainmailArmor
    case IronHelmet
    case PlateArmor
    case LeatherBoots
    case IronBoots
    case LeatherGloves
    case IronGloves
    case BasicSword
    case IronSword

  extension (itemRef: ItemReference) {
    def createEntity(id: String): Entity = itemRef match {
      case ItemReference.HealingPotion => healingPotion(id)
      case ItemReference.FireballScroll => fireballScroll(id)
      case ItemReference.Arrow => arrow(id)
      case ItemReference.Bow => bow(id)
      case ItemReference.YellowKey => key(id, KeyColour.Yellow)
      case ItemReference.BlueKey => key(id, KeyColour.Blue)
      case ItemReference.RedKey => key(id, KeyColour.Red)
      case ItemReference.LeatherHelmet => leatherHelmet(id)
      case ItemReference.ChainmailArmor => chainmailArmor(id)
      case ItemReference.IronHelmet => ironHelmet(id)
      case ItemReference.PlateArmor => plateArmor(id)
      case ItemReference.LeatherBoots => leatherBoots(id)
      case ItemReference.IronBoots => ironBoots(id)
      case ItemReference.LeatherGloves => leatherGloves(id)
      case ItemReference.IronGloves => ironGloves(id)
      case ItemReference.BasicSword => basicSword(id)
      case ItemReference.IronSword => ironSword(id)
    }
  }

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
  
  def snakeSpit(id: String): Entity = Entity(
    id = id,
    NameComponent("Snake Spit", "Venomous ranged attack"),
    UsableItem(EnemyActor(4), ChargeType.InfiniteUse, GameEffect.CreateProjectile(ProjectileReference.SnakeSpit)), // Range 4, infinite use
    Hitbox(),
    Drawable(Sprites.projectileSprite) // Reuse projectile sprite since this is not a pickup item
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
  
  // Equipment items
  def leatherHelmet(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable.armor(EquipmentSlot.Helmet, 1, "Leather Helmet"),
    Hitbox(),
    Drawable(Sprites.leatherHelmetSprite)
  )
  
  def ironHelmet(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable.armor(EquipmentSlot.Helmet, 2, "Iron Helmet"),
    Hitbox(),
    Drawable(Sprites.ironHelmetSprite)
  )
  
  def chainmailArmor(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable.armor(EquipmentSlot.Armor, 1, "Chainmail Armor"),
    Hitbox(),
    Drawable(Sprites.chainmailArmorSprite)
  )
  
  def plateArmor(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable.armor(EquipmentSlot.Armor, 2, "Plate Armor"),
    Hitbox(),
    Drawable(Sprites.plateArmorSprite)
  )
  
  // New equipment items
  def leatherBoots(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable.armor(EquipmentSlot.Boots, 1, "Leather Boots"),
    Hitbox(),
    Drawable(Sprites.leatherBootsSprite)
  )
  
  def ironBoots(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable.armor(EquipmentSlot.Boots, 2, "Iron Boots"),
    Hitbox(),
    Drawable(Sprites.ironBootsSprite)
  )
  
  def leatherGloves(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable.armor(EquipmentSlot.Gloves, 1, "Leather Gloves"),
    Hitbox(),
    Drawable(Sprites.leatherGlovesSprite)
  )
  
  def ironGloves(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable.armor(EquipmentSlot.Gloves, 1, "Iron Gloves"),
    Hitbox(),
    Drawable(Sprites.ironGlovesSprite)
  )
  
  def basicSword(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable.weapon(3, "Basic Sword"),
    Hitbox(),
    Drawable(Sprites.basicSwordSprite)
  )
  
  def ironSword(id: String): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable.weapon(5, "Iron Sword"),
    Hitbox(),
    Drawable(Sprites.ironSwordSprite)
  )
  
  // Generic weapon function for testing
  def weapon(id: String, damage: Int, damageType: game.system.event.GameSystemEvent.DamageSource): Entity = Entity(
    id = id,
    CanPickUp(),
    Equippable.weapon(damage, s"Weapon-$damage"),
    Hitbox(),
    Drawable(Sprites.basicSwordSprite)
  )
}