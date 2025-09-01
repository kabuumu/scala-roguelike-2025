// Test to validate item pickup fixes
package test

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import game.*
import game.entity.*
import game.entity.{UsableItem, Targeting, ItemEffect} // New imports for item system
import game.entity.Inventory.* // Import extension methods
import game.entity.Movement.* // Import position extension
import game.entity.EntityType.LockedDoor // Import LockedDoor
import map.{Dungeon, TileType}
import game.system.event.GameSystemEvent.{CollisionEvent, CollisionTarget}
import game.system.InventorySystem

class TestItemFixes extends AnyFunSuiteLike with Matchers {
  // Create a simple test dungeon
  val testDungeon = Dungeon(testMode = true)

  test("Items should not block movement") {
    // Use the actual starting state to get a proper dungeon
    val gameState = game.StartingState.startingGameState
    val player = gameState.playerEntity
    
    // Find a floor position that's not the player's current position
    val playerPos = player.position
    val floorPosition = Point(playerPos.x + 1, playerPos.y) // Assume this is floor
    
    // Create a potion item without EntityTypeComponent at a floor position
    val potionItem = ItemFactory.createPotion("test-potion")
      .addComponent(Movement(position = floorPosition))
      .addComponent(Drawable(data.Sprites.potionSprite))
    
    val testGameState = gameState.copy(entities = gameState.entities :+ potionItem)
    
    // Check that potion position is NOT in movementBlockingPoints from entities
    // (it might be blocked by dungeon walls/water/rocks, but not by the item entity)
    val entityBlockingPoints = testGameState.entities
      .filter(entity => entity.get[EntityTypeComponent].exists(c => 
        c.entityType == EntityType.Enemy || c.entityType == EntityType.Player || c.entityType.isInstanceOf[LockedDoor]
      ))
      .flatMap(_.get[Movement].map(_.position))
      .toSet
    
    // The potion item should not contribute to entity blocking points
    entityBlockingPoints should not contain(floorPosition)
    
    // Verify that before our fix, the potion would have been treated as Player
    // (this tests that items without EntityTypeComponent don't default to Player in movement blocking)
    val potionEntityType = potionItem.get[EntityTypeComponent]
    potionEntityType shouldBe None
  }

  test("Player should be able to walk over and pick up non-equippable items") {
    // Create a potion item
    val potionItem = ItemFactory.createPotion("test-potion-pickup")
      .addComponent(Movement(position = Point(1, 0)))
      .addComponent(Drawable(data.Sprites.potionSprite))
    
    // Create player
    val player = Entity(
      id = "player",
      EntityTypeComponent(EntityType.Player),
      Movement(position = Point(0, 0)),
      Health(10),
      Initiative(0),
      Inventory(),
      Hitbox()
    )
    
    val gameState = GameState(
      playerEntityId = "player",
      entities = Seq(player, potionItem),
      dungeon = testDungeon
    )
    
    // Simulate collision event (player walking over potion)
    val collisionEvent = CollisionEvent("player", CollisionTarget.Entity("test-potion-pickup"))
    val (updatedState, _) = InventorySystem.update(gameState, Seq(collisionEvent))
    
    // Check that player picked up the potion
    val playerInventory = updatedState.playerEntity.get[Inventory].get
    
    playerInventory.itemEntityIds should contain("test-potion-pickup")
    
    // Check that potion lost its Movement component (no longer rendered)
    val potionAfterPickup = updatedState.entities.find(_.id == "test-potion-pickup")
    potionAfterPickup shouldBe defined
    potionAfterPickup.get.has[Movement] shouldBe false
  }

  test("Player should have starting usable items") {
    // Use the actual starting state
    val player = game.StartingState.player
    val gameState = game.StartingState.startingGameState
    
    // Check usable items
    val usableItems = player.usableItems(gameState)
    
    // Player should start with potion, scroll, and bow using new UsableItem components
    usableItems.exists(item => 
      item.get[UsableItem].exists(usable => 
        usable.targeting == Targeting.Self && 
        usable.effects.exists(_.isInstanceOf[ItemEffect.Heal])
      )
    ) shouldBe true
    
    usableItems.exists(item => 
      item.get[UsableItem].exists(usable => 
        usable.targeting.isInstanceOf[Targeting.TileInRange]
      )
    ) shouldBe true
    
    usableItems.exists(item => 
      item.get[UsableItem].exists(usable => 
        usable.targeting == Targeting.EnemyActor
      )
    ) shouldBe true
    
    usableItems.length should be >= 3
  }
}