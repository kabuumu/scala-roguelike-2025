package data

import data.DeathEvents.DeathEventReference.{GiveExperience, SpawnEntity}
import game.entity.*
import game.entity.Dialogue
import game.Sprite
import game.entity.Conversation
import game.entity.ConversationChoice
import game.entity.ConversationAction.*
import game.entity.Experience.experienceForLevel
import game.entity.Movement.position

object Entities {
  enum EntityReference:
    case Explosion(damage: Int, size: Int)
    case Slimelet
    case Coin
    case Meat
    case Trader

  def explosionEffect(
      creatorId: String,
      position: game.Point,
      targetType: EntityType,
      damage: Int = 10,
      size: Int = 2
  ): Entity = {
    Entity(
      s"Explosion ($creatorId)",
      Hitbox(),
      Collision(damage = damage, persistent = true, targetType, creatorId),
      Movement(position = position),
      Drawable(Sprites.projectileSprite),
      Wave(size),
      EntityTypeComponent(EntityType.Projectile)
    )
  }

  def slimelet(position: game.Point): Entity = {
    val health = 5
    val damage = 1
    Entity(
      Movement(position =
        position
      ), // Position will be set by SpawnEntitySystem
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
      NameComponent(
        "Trader",
        "A friendly merchant willing to buy and sell items"
      ),
      Conversation(
        "Greetings, adventure! Would you like to see my wares?",
        Seq(
          ConversationChoice("Browse goods", TradeAction),
          ConversationChoice("Goodbye", CloseAction)
        )
      ),
      Hitbox(),
      Drawable(Sprites.traderSprite),
      Portrait(Sprite(0, 0, 0))
    )
  }

  def healer(id: String, position: game.Point): Entity = {
    Entity(
      id = id,
      Movement(position = position),
      EntityTypeComponent(EntityType.Trader),
      Healer(healAmount = 100, cost = 50),
      NameComponent("Village Healer", "Can heal your wounds for a price"),
      Conversation(
        "Greetings, traveler. You look wounded. Shall I heal you for 50 gold?",
        Seq(
          ConversationChoice("Heal (50g)", HealAction(100, 50)),
          ConversationChoice("No thanks", CloseAction)
        )
      ),
      Hitbox(),
      Drawable(Sprites.traderSprite),
      Portrait(Sprite(1, 1, 0))
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
      Conversation(
        "Potions and scrolls for the discerning mage! Take a look?",
        Seq(
          ConversationChoice("Browse goods", TradeAction),
          ConversationChoice("Goodbye", CloseAction)
        )
      ),
      Hitbox(),
      Drawable(Sprites.traderSprite),
      Portrait(Sprite(0, 0, 0))
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
      Conversation(
        "Fine steel for your battles. Need a new blade?",
        Seq(
          ConversationChoice("Browse goods", TradeAction),
          ConversationChoice("Goodbye", CloseAction)
        )
      ),
      Hitbox(),
      Drawable(Sprites.traderSprite),
      Portrait(Sprite(0, 1, 0))
    )
  }

  def villager(id: String, position: game.Point): Entity = {
    Entity(
      id = id,
      Movement(position = position),
      NameComponent("Villager", "A simple villager"),
      Conversation(
        "Welcome to our humble village, adventurer! It's not much, but it's home.",
        Seq(ConversationChoice("Goodbye", CloseAction))
      ),
      Hitbox(),
      Drawable(Sprites.playerSprite), // Use player sprite as placeholder
      Portrait(Sprite(1, 0, 0))
    )
  }
  def questGiver(id: String, position: game.Point): Entity = {
    Entity(
      id = id,
      Movement(position = position),
      NameComponent("Elder", "The village elder, looks worried."),
      Conversation(
        "Help! Thieves have stolen our sacred Golden Statue. Can you retrieve it from the cave to the east?",
        Seq(
          ConversationChoice("I will find it.", AcceptQuest("retrieve_statue")),
          ConversationChoice("Maybe later.", CloseAction)
        )
      ),
      Hitbox(),
      Drawable(Sprites.playerSprite),
      Portrait(Sprite(1, 0, 0))
    )
  }
}
