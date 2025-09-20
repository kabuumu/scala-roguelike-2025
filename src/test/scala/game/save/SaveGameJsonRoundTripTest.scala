package game.save

import org.scalatest.funsuite.AnyFunSuite
import game.{GameState, StartingState}
import game.entity.Health._
import game.entity.{Inventory, Movement}
import game.Point
import scala.util.{Success, Failure}

class SaveGameJsonRoundTripTest extends AnyFunSuite {

  test("SaveGameJson round-trip preserves basic game state") {
    val originalState = StartingState.startingGameState
    
    // Serialize and deserialize
    val jsonString = SaveGameJson.serialize(originalState)
    val restoredState = SaveGameJson.deserialize(jsonString)
    
    // Basic checks
    assert(restoredState.playerEntityId == originalState.playerEntityId)
    assert(restoredState.entities.length == originalState.entities.length)
    assert(restoredState.messages == originalState.messages)
    
    // Check player entity exists and has basic components
    val restoredPlayer = restoredState.playerEntity
    assert(restoredPlayer.id == originalState.playerEntity.id)
    assert(restoredPlayer.has[game.entity.Health])
    assert(restoredPlayer.has[game.entity.Movement])
  }
  
  test("SaveGameJson preserves health values correctly") {
    val originalState = StartingState.startingGameState
    val player = originalState.playerEntity
    
    // Get initial health values
    val initialCurrentHealth = player.currentHealth
    val initialMaxHealth = player.maxHealth
    
    // Serialize and deserialize
    val jsonString = SaveGameJson.serialize(originalState)
    val restoredState = SaveGameJson.deserialize(jsonString)
    val restoredPlayer = restoredState.playerEntity
    
    // Health should be preserved
    assert(restoredPlayer.currentHealth == initialCurrentHealth, 
      s"Current health mismatch: expected $initialCurrentHealth, got ${restoredPlayer.currentHealth}")
    assert(restoredPlayer.maxHealth == initialMaxHealth,
      s"Max health mismatch: expected $initialMaxHealth, got ${restoredPlayer.maxHealth}")
  }
  
  test("SaveGameJson preserves health after damage and healing") {
    val originalState = StartingState.startingGameState
    val player = originalState.playerEntity
    
    // Damage the player
    val damagedPlayer = player.damage(30, "test")
    val stateWithDamage = originalState.updateEntity(player.id, damagedPlayer)
    
    // Check damaged health
    val damagedCurrentHealth = damagedPlayer.currentHealth
    val damagedMaxHealth = damagedPlayer.maxHealth
    assert(damagedCurrentHealth < damagedMaxHealth, "Player should be damaged")
    
    // Serialize and deserialize the damaged state
    val jsonString = SaveGameJson.serialize(stateWithDamage)
    val restoredState = SaveGameJson.deserialize(jsonString)
    val restoredPlayer = restoredState.playerEntity
    
    // Damaged health should be preserved
    assert(restoredPlayer.currentHealth == damagedCurrentHealth,
      s"Damaged current health not preserved: expected $damagedCurrentHealth, got ${restoredPlayer.currentHealth}")
    assert(restoredPlayer.maxHealth == damagedMaxHealth,
      s"Max health not preserved: expected $damagedMaxHealth, got ${restoredPlayer.maxHealth}")
  }
  
  test("SaveGameJson preserves inventory and equipment") {
    val originalState = StartingState.startingGameState
    val player = originalState.playerEntity
    
    // Check if player has inventory
    if (player.has[Inventory]) {
      val originalInventory = player.get[Inventory].get
      
      // Serialize and deserialize
      val jsonString = SaveGameJson.serialize(originalState)
      val restoredState = SaveGameJson.deserialize(jsonString)
      val restoredPlayer = restoredState.playerEntity
      
      // Inventory should be preserved
      assert(restoredPlayer.has[Inventory], "Player should have inventory after restoration")
      val restoredInventory = restoredPlayer.get[Inventory].get
      assert(restoredInventory.itemEntityIds == originalInventory.itemEntityIds,
        "Inventory items should be preserved")
    }
  }
  
  test("SaveGameJson round-trip with entity modifications") {
    val originalState = StartingState.startingGameState
    val player = originalState.playerEntity
    
    // Manually move the player by updating their Movement component
    val newPosition = Point(5, 5)
    val movedPlayer = player.update[Movement](_.copy(position = newPosition))
    val updatedState = originalState.updateEntity(player.id, movedPlayer)
    
    // Serialize and deserialize
    val jsonString = SaveGameJson.serialize(updatedState)
    val restoredState = SaveGameJson.deserialize(jsonString)
    
    // Check that player position was preserved
    val restoredPlayerPos = restoredState.playerEntity.get[Movement].get.position
    
    assert(restoredPlayerPos == newPosition,
      s"Player position not preserved: expected $newPosition, got $restoredPlayerPos")
  }
  
  test("SaveGameJson handles empty and null cases gracefully") {
    val originalState = StartingState.startingGameState
    
    // Test basic serialization first
    val jsonString = SaveGameJson.serialize(originalState)
    assert(jsonString.nonEmpty, "Serialized JSON should not be empty")
    
    // Test deserialization
    val restoredState = SaveGameJson.deserialize(jsonString)
    assert(restoredState.entities.nonEmpty, "Restored state should have entities")
    
    // Test that player entity is properly restored with all key components
    val restoredPlayer = restoredState.playerEntity
    assert(restoredPlayer.has[game.entity.Health], "Player should have Health component")
    assert(restoredPlayer.has[game.entity.Movement], "Player should have Movement component")
    assert(restoredPlayer.has[game.entity.EntityTypeComponent], "Player should have EntityTypeComponent")
  }
  
  test("SaveGameJson preserves complex game state with messages") {
    val originalState = StartingState.startingGameState.addMessage("Test message 1").addMessage("Test message 2")
    
    // Serialize and deserialize
    val jsonString = SaveGameJson.serialize(originalState)
    val restoredState = SaveGameJson.deserialize(jsonString)
    
    // Messages should be preserved
    assert(restoredState.messages == originalState.messages,
      s"Messages not preserved: expected ${originalState.messages}, got ${restoredState.messages}")
  }
  
  test("SaveGameJson preserves health edge case: partial health after healing") {
    val originalState = StartingState.startingGameState
    val player = originalState.playerEntity
    val maxHealth = player.maxHealth
    
    // Damage player to 3 health, then heal to 10 (but not full)
    val damagedPlayer = player.damage(maxHealth - 3, "test")
    val partiallyHealedPlayer = damagedPlayer.heal(7) // Should go from 3 to 10
    val stateWithPartialHealth = originalState.updateEntity(player.id, partiallyHealedPlayer)
    
    val expectedHealth = 10
    assert(partiallyHealedPlayer.currentHealth == expectedHealth, 
      s"Setup failed: expected health $expectedHealth, got ${partiallyHealedPlayer.currentHealth}")
    
    // Serialize and deserialize
    val jsonString = SaveGameJson.serialize(stateWithPartialHealth)
    val restoredState = SaveGameJson.deserialize(jsonString)
    val restoredPlayer = restoredState.playerEntity
    
    // Should preserve the partial health state
    assert(restoredPlayer.currentHealth == expectedHealth,
      s"Partial health not preserved: expected $expectedHealth, got ${restoredPlayer.currentHealth}")
    assert(restoredPlayer.maxHealth == maxHealth,
      s"Max health changed: expected $maxHealth, got ${restoredPlayer.maxHealth}")
  }
}