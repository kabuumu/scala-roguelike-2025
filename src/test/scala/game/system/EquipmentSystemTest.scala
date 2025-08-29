package game.system

import data.Sprites
import game.Direction.{Down, Up}
import game.entity.*
import game.entity.EntityType.*
import game.entity.Equipment.*
import game.entity.Health.*
import game.entity.Inventory.*
import game.entity.Movement.*
import game.system.event.GameSystemEvent.*
import game.{GameState, Input, Point}
import map.Dungeon
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import ui.GameController.frameTime
import ui.{GameController, UIState}
import game.status.StatusEffect.addStatusEffect


class EquipmentSystemTest extends AnyFunSuiteLike with Matchers {
  val playerId = "testPlayerId"
  val testDungeon: Dungeon = Dungeon(testMode = true)

  // Create test equipment objects  
  val leatherHelmet = Equippable(EquipmentSlot.Helmet, 2, "Leather Helmet")
  val ironHelmet = Equippable(EquipmentSlot.Helmet, 4, "Iron Helmet")
  val chainmailArmor = Equippable(EquipmentSlot.Armor, 5, "Chainmail Armor")
  val plateArmor = Equippable(EquipmentSlot.Armor, 8, "Plate Armor")

  // Create test weapon entities
  val primaryWeapon = ItemFactory.createWeapon("test-primary-weapon", 10, Melee)

  val basePlayerEntity: Entity = Entity(
    id = playerId,
    Movement(position = Point(4, 4)),
    EntityTypeComponent(EntityType.Player),
    Health(100),
    Initiative(0),
    Inventory(Seq(), Some(primaryWeapon.id), None),
    Equipment(), // Start with empty equipment
    SightMemory(),
    Drawable(Sprites.playerSprite),
    Hitbox(),
    Experience(),
  )

  test("Player should start with empty equipment") {
    val equipment = basePlayerEntity.equipment
    
    equipment.helmet should be(None)
    equipment.armor should be(None)
    equipment.getTotalDamageReduction should be(0)
  }

  test("Equipment component should calculate total damage reduction correctly") {
    val equipment = Equipment(
      helmet = Some(leatherHelmet),
      armor = Some(chainmailArmor)
    )
    
    equipment.getTotalDamageReduction should be(7) // 2 + 5
  }

  test("Equipment component should equip helmet correctly") {
    val emptyEquipment = Equipment()
    val (equippedHelmet, previousItem) = emptyEquipment.equip(ironHelmet)
    
    equippedHelmet.helmet should be(Some(ironHelmet))
    equippedHelmet.armor should be(None)
    equippedHelmet.getTotalDamageReduction should be(4)
    previousItem should be(None)
  }

  test("Equipment component should equip armor correctly") {
    val emptyEquipment = Equipment()
    val (equippedArmor, previousItem) = emptyEquipment.equip(plateArmor)
    
    equippedArmor.helmet should be(None)
    equippedArmor.armor should be(Some(plateArmor))
    equippedArmor.getTotalDamageReduction should be(8)
    previousItem should be(None)
  }

  test("Equipment component should replace existing helmet when equipping new one") {
    val equipmentWithHelmet = Equipment(helmet = Some(leatherHelmet))
    val (updatedEquipment, previousItem) = equipmentWithHelmet.equip(ironHelmet)
    
    updatedEquipment.helmet should be(Some(ironHelmet))
    updatedEquipment.getTotalDamageReduction should be(4) // Iron helmet damage reduction
    previousItem should be(Some(leatherHelmet)) // Should return previously equipped item
  }

  test("Equipment component should unequip helmet correctly") {
    val equipmentWithHelmet = Equipment(helmet = Some(ironHelmet), armor = Some(chainmailArmor))
    val unequippedHelmet = equipmentWithHelmet.unequip(EquipmentSlot.Helmet)
    
    unequippedHelmet.helmet should be(None)
    unequippedHelmet.armor should be(Some(chainmailArmor))
    unequippedHelmet.getTotalDamageReduction should be(5) // Only armor remains
  }

  test("Equipment component should unequip armor correctly") {
    val equipmentWithArmor = Equipment(helmet = Some(ironHelmet), armor = Some(chainmailArmor))
    val unequippedArmor = equipmentWithArmor.unequip(EquipmentSlot.Armor)
    
    unequippedArmor.helmet should be(Some(ironHelmet))
    unequippedArmor.armor should be(None)
    unequippedArmor.getTotalDamageReduction should be(4) // Only helmet remains
  }

  test("Player entity should equip item correctly using extension method") {
    val (playerWithEquipment, previousItem) = basePlayerEntity.equipItemComponent(leatherHelmet)
    
    playerWithEquipment.equipment.helmet should be(Some(leatherHelmet))
    playerWithEquipment.getTotalDamageReduction should be(2)
    previousItem should be(None)
  }

  test("EquipmentSystem should equip adjacent equipment item") {
    // Create a helmet entity adjacent to the player
    val helmetEntity = Entity(
      id = "helmet1",
      Movement(position = Point(4, 3)), // Adjacent to player at (4,4)
      EntityTypeComponent(EntityType.ItemEntity(LeatherHelmet)),
      ItemType(LeatherHelmet),
      CanPickUp(),
      Equippable.fromEquippableItem(LeatherHelmet),
      Hitbox(),
      Drawable(Sprites.leatherHelmetSprite)
    )

    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity, helmetEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val equipEvent = EquipEvent(playerId)
    val (updatedState, _) = EquipmentSystem.update(gameState, Seq(equipEvent))

    // Player should have equipped the helmet
    updatedState.playerEntity.equipment.helmet should be(Some(LeatherHelmet))
    updatedState.playerEntity.getTotalDamageReduction should be(2)
    
    // Helmet entity should still exist but without position (not rendered)
    val helmetEntityAfter = updatedState.entities.find(_.id == "helmet1")
    helmetEntityAfter should not be None
    helmetEntityAfter.get.has[Movement] shouldBe false // Should have no position
    
    // Success message should be added
    updatedState.messages.head should include("Equipped Leather Helmet")
  }

  test("EquipmentSystem should show message when no equipment nearby") {
    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val equipEvent = EquipEvent(playerId)
    val (updatedState, _) = EquipmentSystem.update(gameState, Seq(equipEvent))

    // Player equipment should remain unchanged
    updatedState.playerEntity.equipment.helmet should be(None)
    updatedState.playerEntity.equipment.armor should be(None)
    
    // Error message should be added
    updatedState.messages.head should be("No equippable items nearby")
  }

  test("DamageSystem should reduce damage based on equipment") {
    val (playerWithHelmet, _) = basePlayerEntity.equipItem(LeatherHelmet) // 2 DR
    val (playerWithArmor, _) = playerWithHelmet.equipItem(ChainmailArmor) // 5 DR
    // Total DR: 7

    val attacker = Entity(
      id = "attacker1",
      Movement(position = Point(3, 4)),
      EntityTypeComponent(EntityType.Enemy),
      Health(50),
      Inventory(Seq(), Some(Weapon(15, Melee)), None)
    )

    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerWithArmor, attacker),
      messages = Nil,
      dungeon = testDungeon
    )

    val damageEvent = DamageEvent(playerId, "attacker1", 15) // 15 base damage
    val (updatedState, _) = DamageSystem.update(gameState, Seq(damageEvent))

    // Damage should be reduced: 15 - 7 = 8 damage dealt
    // Player health: 100 - 8 = 92
    updatedState.playerEntity.currentHealth should be(92)
  }

  test("DamageSystem should ensure minimum 1 damage even with high armor") {
    val (playerWithHelmet, _) = basePlayerEntity.equipItem(IronHelmet) // 4 DR
    val (playerWithMaxArmor, _) = playerWithHelmet.equipItem(PlateArmor) // 8 DR
    // Total DR: 12

    val weakAttacker = Entity(
      id = "weakAttacker",
      Movement(position = Point(3, 4)),
      EntityTypeComponent(EntityType.Enemy),
      Health(50),
      Inventory(Seq(), Some(Weapon(5, Melee)), None)
    )

    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerWithMaxArmor, weakAttacker),
      messages = Nil,
      dungeon = testDungeon
    )

    val damageEvent = DamageEvent(playerId, "weakAttacker", 5) // 5 base damage vs 12 DR
    val (updatedState, _) = DamageSystem.update(gameState, Seq(damageEvent))

    // Minimum 1 damage should be dealt: 100 - 1 = 99
    updatedState.playerEntity.currentHealth should be(99)
  }

  test("DamageSystem should combine equipment and status effect damage reduction") {
    import game.status.StatusEffect
    import game.status.StatusEffect.EffectType.ReduceIncomingDamage

    val fortifiedPerk = StatusEffect(
      ReduceIncomingDamage(3),
      name = "Fortified",
      description = "Reduces incoming damage by 3."
    )

    val (playerWithArmor, _) = basePlayerEntity.equipItem(ChainmailArmor) // 5 DR from equipment
    val playerWithArmorAndPerk = playerWithArmor.addStatusEffect(fortifiedPerk) // 3 DR from perk
    // Total DR: 8

    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerWithArmorAndPerk),
      messages = Nil,
      dungeon = testDungeon
    )

    val damageEvent = DamageEvent(playerId, "attacker", 20) // 20 base damage
    val (updatedState, _) = DamageSystem.update(gameState, Seq(damageEvent))

    // Damage should be reduced: 20 - 8 = 12 damage dealt
    // Player health: 100 - 12 = 88
    updatedState.playerEntity.currentHealth should be(88)
  }

  test("Player should equip equipment via GameController using Q key") {
    // Create armor entity adjacent to player
    val armorEntity = Entity(
      id = "armor1",
      Movement(position = Point(5, 4)), // Adjacent to player at (4,4)
      EntityTypeComponent(EntityType.ItemEntity(PlateArmor)),
      ItemType(PlateArmor),
      CanPickUp(),
      Equippable.fromEquippableItem(PlateArmor),
      Hitbox(),
      Drawable(Sprites.plateArmorSprite)
    )

    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity, armorEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val gameController = GameController(UIState.Move, gameState)
    val updatedController = gameController.update(Some(Input.Equip), frameTime)

    // Player should have equipped the armor
    updatedController.gameState.playerEntity.equipment.armor should be(Some(PlateArmor))
    updatedController.gameState.playerEntity.getTotalDamageReduction should be(8)
    
    // Armor entity should still exist but without position (not rendered)
    val armorEntityAfter = updatedController.gameState.entities.find(_.id == "armor1")
    armorEntityAfter should not be None
    armorEntityAfter.get.has[Movement] shouldBe false // Should have no position
    
    // Success message should be present
    updatedController.gameState.messages.head should include("Equipped Plate Armor")
  }

  test("Equipment should provide different damage reduction values") {
    LeatherHelmet.damageReduction should be(2)
    IronHelmet.damageReduction should be(4)
    ChainmailArmor.damageReduction should be(5)
    PlateArmor.damageReduction should be(8)
  }

  test("Equipment should have correct slot assignments") {
    LeatherHelmet.slot should be(EquipmentSlot.Helmet)
    IronHelmet.slot should be(EquipmentSlot.Helmet)
    ChainmailArmor.slot should be(EquipmentSlot.Armor)
    PlateArmor.slot should be(EquipmentSlot.Armor)
  }

  test("InventorySystem should NOT auto-pickup equippable items when walked over") {
    // Create a helmet entity at the same position as the player
    val helmetEntity = Entity(
      id = "helmet1",
      Movement(position = Point(4, 4)), // Same position as player
      EntityTypeComponent(EntityType.ItemEntity(LeatherHelmet)),
      ItemType(LeatherHelmet),
      CanPickUp(),
      Equippable.fromEquippableItem(LeatherHelmet),
      Hitbox(),
      Drawable(Sprites.leatherHelmetSprite)
    )

    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity, helmetEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    // Simulate collision event (player walking over equipment)
    val collisionEvent = CollisionEvent(playerId, CollisionTarget.Entity("helmet1"))
    val (updatedState, _) = InventorySystem.update(gameState, Seq(collisionEvent))

    // Player should NOT have picked up the helmet
    updatedState.playerEntity.items(updatedState) should not contain LeatherHelmet
    updatedState.playerEntity.equipment.helmet should be(None)
    
    // Helmet entity should still exist in the world
    updatedState.entities.find(_.id == "helmet1") should not be None
  }

  test("InventorySystem should still auto-pickup non-equippable items when walked over") {
    // Create a potion entity at the same position as the player
    val potionEntity = Entity(
      id = "potion1",
      Movement(position = Point(4, 4)), // Same position as player
      EntityTypeComponent(EntityType.ItemEntity(Potion)),
      ItemType(Potion),
      CanPickUp(),
      Hitbox(),
      Drawable(Sprites.potionSprite)
    )

    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(basePlayerEntity, potionEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    // Simulate collision event (player walking over potion)
    val collisionEvent = CollisionEvent(playerId, CollisionTarget.Entity("potion1"))
    val (updatedState, _) = InventorySystem.update(gameState, Seq(collisionEvent))

    // Player should have picked up the potion
    updatedState.playerEntity.items(updatedState) should contain(Potion)
    
    // Potion entity should still exist but without position (not rendered)
    val potionEntityAfter = updatedState.entities.find(_.id == "potion1")
    potionEntityAfter should not be None
    potionEntityAfter.get.has[Movement] shouldBe false // Should have no position
  }

  test("EquipmentSystem should drop currently equipped item when equipping new item to same slot") {
    // Create a player with already equipped leather helmet
    val (playerWithHelmet, _) = basePlayerEntity.equipItem(LeatherHelmet)
    
    // Create an iron helmet entity adjacent to player
    val ironHelmetEntity = Entity(
      id = "ironHelmet1",
      Movement(position = Point(4, 3)), // Adjacent to player at (4,4)
      EntityTypeComponent(EntityType.ItemEntity(IronHelmet)),
      ItemType(IronHelmet),
      CanPickUp(),
      Equippable.fromEquippableItem(IronHelmet),
      Hitbox(),
      Drawable(Sprites.ironHelmetSprite)
    )

    val gameState = GameState(
      playerEntityId = playerId,
      entities = Seq(playerWithHelmet, ironHelmetEntity),
      messages = Nil,
      dungeon = testDungeon
    )

    val equipEvent = EquipEvent(playerId)
    val (updatedState, _) = EquipmentSystem.update(gameState, Seq(equipEvent))

    // Player should now have the iron helmet equipped
    updatedState.playerEntity.equipment.helmet should be(Some(IronHelmet))
    updatedState.playerEntity.getTotalDamageReduction should be(4)
    
    // Iron helmet entity should still exist but without position (not rendered)
    val ironHelmetEntityAfter = updatedState.entities.find(_.id == "ironHelmet1")
    ironHelmetEntityAfter should not be None
    ironHelmetEntityAfter.get.has[Movement] shouldBe false // Should have no position
    
    // Leather helmet should be dropped at the position where iron helmet was (4, 3)
    val droppedHelmetEntity = updatedState.entities.find { e =>
      e.position == Point(4, 3) && e.entityType == EntityType.ItemEntity(LeatherHelmet)
    }
    droppedHelmetEntity should not be None
    droppedHelmetEntity.get.entityType should be(EntityType.ItemEntity(LeatherHelmet))
    
    // Success message should be added
    updatedState.messages.head should include("Equipped Iron Helmet")
  }
}
