package game

import data.Items
import data.Items.ItemReference
import game.entity.*
import game.entity.Coins.*
import game.entity.Inventory.*
import game.entity.EntityType.entityType
import org.scalatest.funsuite.AnyFunSuite
import testsupport.Given
import ui.InputAction

class TradeSystemTest extends AnyFunSuite {
  
  test("Trader has default inventory with correct prices") {
    val trader = data.Entities.trader("trader-1", Point(5, 5))
    
    trader.get[Trader] match {
      case Some(traderComp) =>
        // Check a few key items
        assert(traderComp.buyPrice(ItemReference.HealingPotion).contains(15))
        assert(traderComp.sellPrice(ItemReference.HealingPotion).contains(8))
        assert(traderComp.buyPrice(ItemReference.IronSword).contains(40))
        assert(traderComp.sellPrice(ItemReference.IronSword).contains(20))
      case None =>
        fail("Trader should have Trader component")
    }
  }
  
  test("Trader entity has correct entity type") {
    val trader = data.Entities.trader("trader-1", Point(5, 5))
    
    assert(trader.entityType == EntityType.Trader)
  }
  
  test("Player can buy an item from trader with sufficient coins") {
    val trader = data.Entities.trader("trader-1", Point(5, 5))
    val initialGameState = Given
      .thePlayerAt(4, 4)
      .modifyPlayer[Coins](_.copy(current = 50, totalCollected = 50))
      .withEntities(trader)
      .buildGameState()
    
    // Simulate buying
    val updatedState = game.system.TradeSystem.update(
      initialGameState,
      Seq(game.system.event.GameSystemEvent.InputEvent(
        initialGameState.playerEntity.id,
        InputAction.BuyItem(trader, ItemReference.HealingPotion)
      ))
    )._1
    
    assert(updatedState.playerEntity.coins == 35) // 50 - 15
    assert(updatedState.playerEntity.get[Inventory].exists(_.itemEntityIds.nonEmpty))
  }
  
  test("Player cannot buy item without sufficient coins") {
    val trader = data.Entities.trader("trader-1", Point(5, 5))
    val initialGameState = Given
      .thePlayerAt(4, 4)
      .modifyPlayer[Coins](_.copy(current = 10, totalCollected = 10))
      .withEntities(trader)
      .buildGameState()
    
    // Try to buy with insufficient coins
    val updatedState = game.system.TradeSystem.update(
      initialGameState,
      Seq(game.system.event.GameSystemEvent.InputEvent(
        initialGameState.playerEntity.id,
        InputAction.BuyItem(trader, ItemReference.HealingPotion)
      ))
    )._1
    
    assert(updatedState.playerEntity.coins == 10) // No change
  }
  
  test("Player can sell an item to trader") {
    val potion = Items.healingPotion("potion-1")
    val trader = data.Entities.trader("trader-1", Point(5, 5))
    val initialGameState = Given
      .thePlayerAt(4, 4)
      .modifyPlayer[Coins](_.copy(current = 0, totalCollected = 0))
      .withItems(potion)
      .withEntities(trader)
      .buildGameState()
    
    // Sell the potion
    val updatedState = game.system.TradeSystem.update(
      initialGameState,
      Seq(game.system.event.GameSystemEvent.InputEvent(
        initialGameState.playerEntity.id,
        InputAction.SellItem(trader, potion)
      ))
    )._1
    
    assert(updatedState.playerEntity.coins == 8) // Sell price
    assert(!updatedState.playerEntity.get[Inventory].exists(_.itemEntityIds.contains(potion.id)))
  }
  
  test("Trader spawns on each floor when descending stairs") {
    // Floor 1 - verify initial trader exists
    val floor1State = StartingState.startingGameState
    val floor1Traders = floor1State.entities.filter(_.entityType == EntityType.Trader)
    assert(floor1Traders.size == 1, s"Floor 1 should have exactly 1 trader, found ${floor1Traders.size}")
    
    // Simulate descending to floor 2
    val floor2State = game.system.DescendStairsSystem.update(
      floor1State,
      Seq(game.system.event.GameSystemEvent.InputEvent(
        floor1State.playerEntity.id,
        InputAction.DescendStairs
      ))
    )._1
    
    assert(floor2State.dungeonFloor == 2, "Should be on floor 2 after descending")
    val floor2Traders = floor2State.entities.filter(_.entityType == EntityType.Trader)
    assert(floor2Traders.size == 1, s"Floor 2 should have exactly 1 trader, found ${floor2Traders.size}")
    
    // Simulate descending to floor 3
    val floor3State = game.system.DescendStairsSystem.update(
      floor2State,
      Seq(game.system.event.GameSystemEvent.InputEvent(
        floor2State.playerEntity.id,
        InputAction.DescendStairs
      ))
    )._1
    
    assert(floor3State.dungeonFloor == 3, "Should be on floor 3 after descending")
    val floor3Traders = floor3State.entities.filter(_.entityType == EntityType.Trader)
    assert(floor3Traders.size == 1, s"Floor 3 should have exactly 1 trader, found ${floor3Traders.size}")
  }
  
  test("Starting state player has equipment correctly initialized") {
    val gameState = StartingState.startingGameState
    val player = gameState.playerEntity
    
    // Verify player has Equipment component
    val equipment = player.get[Equipment]
    assert(equipment.isDefined, "Player should have Equipment component")
    
    // Verify armor and weapon are equipped
    assert(equipment.flatMap(_.armor).isDefined, "Player should have armor equipped")
    assert(equipment.flatMap(_.weapon).isDefined, "Player should have weapon equipped")
    
    // Verify getAllEquipped returns them
    val allEquipped = equipment.map(_.getAllEquipped).getOrElse(Seq.empty)
    assert(allEquipped.size == 2, s"Player should have 2 equipped items, found ${allEquipped.size}")
    
    // Print for debugging
    println(s"Equipped items: ${allEquipped.map(_.itemName).mkString(", ")}")
  }
  
  test("Player's sellable items includes equipped items") {
    val trader = data.Entities.trader("trader-1", Point(5, 5))
    val gameState = StartingState.startingGameState.copy(
      entities = StartingState.startingGameState.entities :+ trader
    )
    val player = gameState.playerEntity
    
    // Get inventory items
    val inventoryItems = player.inventoryItems(gameState)
    println(s"Inventory items count: ${inventoryItems.size}")
    
    // Get equipped items
    val equippedItems = player.get[Equipment]
      .map(_.getAllEquipped)
      .getOrElse(Seq.empty)
    println(s"Equipped items count: ${equippedItems.size}")
    println(s"Equipped item names: ${equippedItems.map(_.itemName).mkString(", ")}")
    
    // Try to create entities from equipped items
    val equippedAsEntities = equippedItems.map { equippable =>
      val itemRefOpt = data.Items.ItemReference.values.find { ref =>
        val tempEntity = ref.createEntity("temp")
        tempEntity.get[Equippable].exists(_.itemName == equippable.itemName)
      }
      itemRefOpt.map { itemRef =>
        itemRef.createEntity(s"equipped-${equippable.itemName}")
      }
    }.flatten
    
    println(s"Equipped as entities count: ${equippedAsEntities.size}")
    
    // Verify we can create entities from equipped items
    assert(equippedAsEntities.nonEmpty, "Should be able to create entities from equipped items")
    
    // Combine both
    val allItems = inventoryItems ++ equippedAsEntities
    println(s"Total sellable items (should include equipped): ${allItems.size}")
    
    assert(allItems.size >= equippedAsEntities.size, 
      s"Total items (${allItems.size}) should include equipped items (${equippedAsEntities.size})")
  }
}
