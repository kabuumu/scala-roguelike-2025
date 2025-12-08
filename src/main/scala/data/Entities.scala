package data

import data.DeathEvents.DeathEventReference.{GiveExperience, SpawnEntity}
import game.entity.*
import game.entity.Experience.experienceForLevel
import game.entity.Movement.position

object Entities {
  enum EntityReference:
    case Explosion(damage: Int, size: Int)
    case Slimelet
    case Coin
    case Trader

  def explosionEffect(creatorId: String, position: game.Point, targetType: EntityType, damage: Int = 10, size: Int = 2): Entity = {
    Entity(
      s"Explosion ($creatorId)",
      Hitbox(),
      Collision(damage = damage, persistent = true, targetType, creatorId),
      Movement(position = position),
      Drawable(Sprites.projectileSprite),
      Wave(size),
      EntityTypeComponent(EntityType.Projectile),
    )
  }
  
  def slimelet(position: game.Point): Entity = {
    val health = 5 
    val damage = 1
    Entity(
      Movement(position = position), // Position will be set by SpawnEntitySystem
      EntityTypeComponent(EntityType.Enemy),
      Health(10),
      Initiative(8),
      Inventory(Nil), // No weapon for slimelets, they use default 1 damage
      EventMemory(),
      Drawable(Sprites.slimeletSprite),
      Hitbox(),
      DeathEvents(
        Seq(
          GiveExperience(experienceForLevel(2) / 5)
        )
      )
    )
  }
  
  def trader(id: String, position: game.Point): Entity = {
    Entity(
      id = id,
      Movement(position = position),
      EntityTypeComponent(EntityType.Trader),
      Trader(Trader.defaultInventory),
      NameComponent("Trader", "A friendly merchant willing to buy and sell items"),
      Hitbox(),
      Drawable(Sprites.traderSprite)
    )
  }

  def healer(id: String, position: game.Point): Entity = {
    val inventory = Map(
      Items.ItemReference.HealingService -> (50, 0) // Cost 50, cannot sell back
    )
    Entity(
      id = id,
      Movement(position = position),
      EntityTypeComponent(EntityType.Trader),
      Trader(inventory),
      NameComponent("Village Healer", "Can heal your wounds for a price"),
      Hitbox(),
      Drawable(Sprites.traderSprite) // Use same sprite for now
    )
  }

  def potionMerchant(id: String, position: game.Point): Entity = {
    val inventory = Map(
      Items.ItemReference.HealingPotion -> (15, 8),
      Items.ItemReference.FireballScroll -> (25, 12)
    )
    Entity(
      id = id,
      Movement(position = position),
      EntityTypeComponent(EntityType.Trader),
      Trader(inventory),
      NameComponent("Alchemist", "Sells potions and scrolls"),
      Hitbox(),
      Drawable(Sprites.traderSprite)
    )
  }

  def equipmentMerchant(id: String, position: game.Point): Entity = {
    val inventory = Map(
      Items.ItemReference.Arrow -> (5, 2),
      Items.ItemReference.Bow -> (40, 20),
      Items.ItemReference.LeatherHelmet -> (20, 10),
      Items.ItemReference.ChainmailArmor -> (30, 15),
      Items.ItemReference.IronHelmet -> (35, 17),
      Items.ItemReference.PlateArmor -> (50, 25),
      Items.ItemReference.LeatherBoots -> (20, 10),
      Items.ItemReference.IronBoots -> (35, 17),
      Items.ItemReference.LeatherGloves -> (20, 10),
      Items.ItemReference.IronGloves -> (35, 17),
      Items.ItemReference.BasicSword -> (25, 12),
      Items.ItemReference.IronSword -> (40, 20)
    )
    Entity(
      id = id,
      Movement(position = position),
      EntityTypeComponent(EntityType.Trader),
      Trader(inventory),
      NameComponent("Blacksmith", "Sells weapons and armor"),
      Hitbox(),
      Drawable(Sprites.traderSprite)
    )
  }

  def villager(id: String, position: game.Point): Entity = {
    Entity(
      id = id,
      Movement(position = position),
      NameComponent("Villager", "A simple villager"),
      Dialogue("Welcome to our humble village, adventurer!"),
      Hitbox(),
      Drawable(Sprites.playerSprite) // Use player sprite as placeholder
    )
  }
}
