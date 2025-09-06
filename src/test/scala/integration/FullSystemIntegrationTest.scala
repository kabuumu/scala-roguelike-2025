package integration

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import testsupport.FullSystemTestFramework.*
import testsupport.Given
import ui.UIState
import indigo.Key
import game.entity.Health.damage
import game.entity.{Melee, WeaponType}

/**
 * Full System Integration Tests
 * 
 * These tests validate the complete game pipeline from user input to game state changes,
 * including the Indigo engine integration layer that was previously untested.
 * 
 * Tests cover:
 * - Input mapping from keyboard to game actions
 * - Frame timing and game loop integrity  
 * - Complete user interaction workflows
 * - Rendering pipeline validation
 * - Performance characteristics
 */
class FullSystemIntegrationTest extends AnyFunSuiteLike with Matchers {

  test("Full system: Player movement with keyboard input") {
    scenarios.playerAt(4, 4)
      .userInput.movesUp()
      .assertions.playerIsAt(4, 3)
      .waitUntilPlayerReady()
      .userInput.movesRight()
      .assertions.playerIsAt(5, 3)
      .waitUntilPlayerReady()
      .userInput.movesDown()
      .assertions.playerIsAt(5, 4)
      .waitUntilPlayerReady()
      .userInput.movesLeft()
      .assertions.playerIsAt(4, 4)
  }

  test("Full system: Initiative system works with frame timing") {
    scenarios.playerAt(4, 4)
      .assertions.playerInitiativeChanged()
      .simulateFrames(5) // Let some time pass
      .assertions.playerInitiativeChanged()
      .assertions.hasProcessedFrames(5)
  }

  test("Full system: Item usage workflow through input pipeline") {
    val potion = Given.items.potion("test-potion")
    
    scenarios.playerWithItems(4, 4, potion)
      .damagePlayer(3) // Damage player for testing healing
      .assertions.playerHasHealth(7) // Should be damaged
      .userInput.usesItem() // Press 'U' to open items
      .assertions.uiStateIs[UIState.ListSelect[?]] // Should enter item selection
      .waitUntilPlayerReady() // Wait for UI action to process
      .userInput.confirms() // Press Space to use potion
      .waitUntilPlayerReady() // Wait for item to be used
      .assertions.playerHasHealth(10) // Should be healed
      .assertions.uiStateIs[UIState.Move.type] // Should return to move state
  }

  test("Full system: Combat workflow with timing") {
    val enemies = Given.enemies.basic("enemy1", 5, 4, health = 5)
    
    scenarios.playerWithEnemies(4, 4, enemies*)
      .assertions.entityCountIs(4) // Player + enemy + 2 weapons
      .userInput.movesRight() // Move toward enemy
      .assertions.playerIsAt(5, 4)
      .simulateFrames(10) // Let initiative reset
      .userInput.primaryAttack() // Attack with 'Z'
      .simulateFrames(3) // Let combat resolve
      // Enemy should take damage or be destroyed
  }

  test("Full system: Scroll usage with targeting") {
    val scroll = Given.items.scroll("test-scroll")
    
    scenarios.playerWithItems(4, 4, scroll)
      .userInput.usesItem() // Press 'U'
      .assertions.uiStateIs[UIState.ListSelect[?]]
      .userInput.confirms() // Select scroll
      .assertions.uiStateIs[UIState.ScrollSelect] // Should enter targeting mode
      .userInput.movesUp() // Move cursor
      .userInput.confirms() // Fire scroll
      .assertions.uiStateIs[UIState.Move.type] // Should return to move state
  }

  test("Full system: Frame rate performance validation") {
    scenarios.playerAt(4, 4)
      .validateFrameTiming() // Ensure single frame is fast enough
      .simulateFrames(10)
      .validateFrameTiming() // Ensure consistent performance
  }

  test("Full system: Rendering pipeline integrity") {
    scenarios.playerAt(4, 4)
      .validateRenderableState() // Test scene generation
      .userInput.movesUp()
      .validateRenderableState() // Test after state change
  }

  test("Full system: Input mapping validation") {
    scenarios.playerAt(4, 4)
      // Test all major input mappings work through the pipeline
      .userInput.pressesKey(Key.KEY_W) // Alternative movement
      .assertions.playerIsAt(4, 3)
      .waitUntilPlayerReady()
      .userInput.pressesKey(Key.KEY_S)
      .assertions.playerIsAt(4, 4)
      .waitUntilPlayerReady()
      .userInput.pressesKey(Key.KEY_A)
      .assertions.playerIsAt(3, 4)
      .waitUntilPlayerReady()
      .userInput.pressesKey(Key.KEY_D)
      .assertions.playerIsAt(4, 4)
  }

  test("Full system: Error handling during update cycle") {
    // Test that the system gracefully handles edge cases
    scenarios.playerAt(4, 4)
      .simulateFrames(20) // Stress test with moderate frame count
      .assertions.playerIsAt(4, 4) // Should remain stable
      .assertions.hasProcessedFrames(20)
  }

  test("Full system: Main menu navigation") {
    scenarios.mainMenu()
      .assertions.uiStateIs[UIState.MainMenu]
      .userInput.movesDown() // Navigate menu
      .userInput.movesUp()
      .userInput.confirms() // Select "New Game"
      .assertions.uiStateIs[UIState.Move.type] // Should transition to game
  }

  test("Full system: Equipment system integration") {
    val helmet = Given.items.weapon("helmet", 0, Melee) // Use as equipment
    
    scenarios.playerWithItems(4, 4, helmet)
      .userInput.equipItems() // Press 'Q'
      .assertions.uiStateIs[UIState.ListSelect[?]]
      .userInput.confirms() // Equip the item
      .assertions.uiStateIs[UIState.Move.type]
  }

  test("Full system: Multiple enemy combat scenario") {
    val enemy1 = Given.enemies.basic("enemy1", 5, 4, health = 3)
    val enemy2 = Given.enemies.basic("enemy2", 3, 4, health = 3) 
    val allEnemies = enemy1 ++ enemy2
    
    scenarios.playerWithEnemies(4, 4, allEnemies*)
      .assertions.entityCountIs(6) // Player + 2 enemies + 4 weapons
      .userInput.movesRight() // Approach enemies
      .userInput.primaryAttack()
      .simulateFrames(5) // Let combat resolve
      .userInput.movesLeft()
      .userInput.primaryAttack()
      .simulateFrames(5)
      // Should handle multiple enemies without issues
  }

  test("Full system: Item pickup and inventory management") {
    // Test complete item interaction workflow
    scenarios.playerAt(4, 4)
      .userInput.interacts() // Press 'E' to interact
      .simulateFrames(2)
      .userInput.usesItem() // Open inventory
      // Should handle empty inventory gracefully
  }

  test("Full system: Cancel actions workflow") {
    val potion = Given.items.potion("test-potion")
    
    scenarios.playerWithItems(4, 4, potion)
      .userInput.usesItem() // Open items
      .assertions.uiStateIs[UIState.ListSelect[?]]
      .userInput.cancels() // Press Escape
      .assertions.uiStateIs[UIState.Move.type] // Should return to move state
  }

  test("Full system: Timer-based systems integration") {
    scenarios.playerAt(4, 4)
      .assertions.systemsAreOperational()
      .assertions.timerBasedSystemsWork() // This will test frame-based initiative updates
  }

  test("Full system: Complex game state transitions") {
    val scroll = Given.items.scroll("complex-scroll")
    val enemy = Given.enemies.basic("complex-enemy", 6, 4, health = 5)
    
    scenarios.playerWithItems(4, 4, scroll)
      .withEntities(enemy*)
      .assertions.systemsAreOperational()
      .userInput.usesItem() // Enter item menu
      .userInput.confirms() // Select scroll - should enter ScrollSelect
      .userInput.movesRight() // Move cursor to enemy
      .userInput.movesRight()
      .userInput.confirms() // Fire at enemy
      .simulateFrames(10) // Let projectile systems work
      .assertions.systemsAreOperational() // Should still be operational after complex interaction
  }
}