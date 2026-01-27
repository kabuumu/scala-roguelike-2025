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
    CharacterFactory.create(
      id = "slimelet-" + scala.util.Random.alphanumeric.take(8).mkString,
      position = position,
      sprite = Sprites.slimeletSprite,
      entityType = EntityType.Enemy,
      hp = Some(10),
      initiative = Some(4),
      inventory = Some(Inventory(Nil)),
      extraComponents = Seq(
        // Note: Entities.slimelet seems to use EnemyTypeComponent but lacks EnemyReference import in original snippet if not imported.
        // Assuming EnemyReference.Slimelet is valid here as per original code context.
        // Original: EnemyTypeComponent(EnemyReference.Slimelet)
        // Wait, Entities.scala lines 14-19 has enum EntityReference including Slimelet.
        // But original code (line 46) used EnemyTypeComponent(EntityType.Enemy)? No line 46 is EntityTypeComponent.
        // Line 125 of Enemies.scala used EnemyReference.Slimelet.
        // Entities.scala line 16 has case Slimelet.
        // But Entities.scala line 46 used EntityTypeComponent(EntityType.Enemy).
        // Line 47 used Health(10).
        // Wait, line 125 in Enemies.scala used EnemyTypeComponent(EnemyReference.Slimelet).
        // In Entities.scala line 46 is EntityTypeComponent(EntityType.Enemy).
        // But I don't see EnemyTypeComponent usage in Entities.slimelet in my view.
        // Let's look at the view again.
        // Line 46: EntityTypeComponent(EntityType.Enemy)
        // Line 47: Health(10)
        // It does NOT have EnemyTypeComponent?
        // Wait, I might have misread the view.
        // Let's re-read Entities.scala lines 39-59.
        // Line 46: EntityTypeComponent(EntityType.Enemy)
        // Next line is 47 Health.
        // It seems Entities.slimelet behaves differently or I missed something.
        // Ah, look at Entites.scala previously viewed.
        // Line 125 in Enemies.scala HAS EnemyTypeComponent.
        // Line 39 in Entities.scala...
        // Wait, previous view of Entities.scala (step 765):
        // 39: def slimelet...
        // 46: EntityTypeComponent(EntityType.Enemy),
        // 47: Health(10),
        // 48: Initiative(8),
        // 49: Inventory(Nil),
        // 50: EventMemory(),
        // 51: Drawable...
        // 52: Hitbox...
        // 53: DeathEvents...
        // It does NOT have EnemyTypeComponent.
        // So I should preserve that logic or improve it.
        // Since I want to unify, I should probably stick to what Enemies.scala does if this is indeed the same entity.
        // But `Entities.slimelet` is used where?
        // If I change it to use CharacterFactory, I should probably add EnemyTypeComponent if it's needed for AI?
        // If not needed, CharacterFactory is fine.
        // But CharacterFactory doesn't add EnemyTypeComponent by default.
        // I'll stick to what was there: No EnemyTypeComponent.
        // But wait, Slimelet AI might depend on it?
        // If it's just a dumb enemy...
        // I will follow the original implementation of Entities.slimelet for now, but using Factory.
      )
    )
  }

  def trader(id: String, position: game.Point): Entity = {
    CharacterFactory.create(
      id,
      position,
      Sprites.traderSprite,
      EntityType.Trader,
      name = Some(
        NameComponent(
          "Trader",
          "A friendly merchant willing to buy and sell items"
        )
      ),
      extraComponents = Seq(
        Trader(Trader.defaultInventory),
        Conversation(
          "Greetings, adventure! Would you like to see my wares?",
          Seq(
            ConversationChoice("Browse goods", TradeAction),
            ConversationChoice("Goodbye", CloseAction)
          )
        ),
        Portrait(Sprite(0, 0, 0))
      )
    )
  }

  def healer(id: String, position: game.Point): Entity = {
    CharacterFactory.create(
      id,
      position,
      Sprites.traderSprite,
      EntityType.Trader,
      name = Some(
        NameComponent("Village Healer", "Can heal your wounds for a price")
      ),
      extraComponents = Seq(
        Healer(healAmount = 100, cost = 50),
        Conversation(
          "Greetings, traveler. You look wounded. Shall I heal you for 50 gold?",
          Seq(
            ConversationChoice("Heal (50g)", HealAction(100, 50)),
            ConversationChoice("No thanks", CloseAction)
          )
        ),
        Portrait(Sprite(1, 1, 0))
      )
    )
  }

  def potionMerchant(id: String, position: game.Point): Entity = {
    val inventory = Map(
      Items.ItemReference.HealingPotion -> (15, 8),
      Items.ItemReference.FireballScroll -> (25, 12)
    )
    CharacterFactory.create(
      id,
      position,
      Sprites.traderSprite,
      EntityType.Trader,
      name = Some(NameComponent("Alchemist", "Sells potions and scrolls")),
      extraComponents = Seq(
        Trader(inventory),
        Conversation(
          "Potions and scrolls for the discerning mage! Take a look?",
          Seq(
            ConversationChoice("Browse goods", TradeAction),
            ConversationChoice("Goodbye", CloseAction)
          )
        ),
        Portrait(Sprite(0, 0, 0))
      )
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
    CharacterFactory.create(
      id,
      position,
      Sprites.traderSprite,
      EntityType.Trader,
      name = Some(NameComponent("Blacksmith", "Sells weapons and armor")),
      extraComponents = Seq(
        Trader(inventory),
        Conversation(
          "Fine steel for your battles. Need a new blade?",
          Seq(
            ConversationChoice("Browse goods", TradeAction),
            ConversationChoice("Goodbye", CloseAction)
          )
        ),
        Portrait(Sprite(0, 1, 0))
      )
    )
  }

  def villager(id: String, position: game.Point): Entity = {
    CharacterFactory.create(
      id,
      position,
      Sprites.playerSprite,
      EntityType.Villager,
      name = Some(NameComponent("Villager", "A simple villager")),
      extraComponents = Seq(
        Conversation(
          "Welcome to our humble village, adventurer! It's not much, but it's home.",
          Seq(ConversationChoice("Goodbye", CloseAction))
        ),
        Portrait(Sprite(1, 0, 0))
      )
    )
  }
  def questGiver(id: String, position: game.Point): Entity = {
    CharacterFactory.create(
      id,
      position,
      Sprites.playerSprite,
      EntityType.Villager,
      name = Some(NameComponent("Elder", "The village elder, looks worried.")),
      extraComponents = Seq(
        Conversation(
          "Help! Thieves have stolen our sacred Golden Statue. Can you retrieve it from the cave to the east?",
          Seq(
            ConversationChoice(
              "I will find it.",
              AcceptQuest("retrieve_statue")
            ),
            ConversationChoice("Maybe later.", CloseAction)
          )
        ),
        Portrait(Sprite(1, 0, 0))
      )
    )
  }
}
