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
 * Essential System Integration Tests
 * 
 * Focused integration tests that validate critical system boundaries and interactions.
 * These tests complement the comprehensive gameplay scenarios by testing specific
 * technical integration points that require precise validation.
 * 
 * Focus areas:
 * - Input mapping and responsiveness
 * - Frame timing and performance
 * - UI state transitions
 * - Core system stability
 */
class FullSystemIntegrationTest extends AnyFunSuiteLike with Matchers {

  test("Input mapping and responsiveness validation") {
    // Tests that all input types are correctly mapped and processed
    scenarios.playerAt(4, 4)
      .userInput.pressesKey(Key.KEY_W) // WASD movement
      .assertions.playerIsAt(4, 3)
      .waitUntilPlayerReady()
      .userInput.pressesKey(Key.ARROW_RIGHT) // Arrow key movement
      .assertions.playerIsAt(5, 3)
      .waitUntilPlayerReady()
      .userInput.pressesKey(Key.KEY_Z) // Combat key
      .waitUntilPlayerReady()
      .assertions.systemsAreOperational()
  }

  test("Frame timing and performance characteristics") {
    // Validates that the game maintains stable performance
    scenarios.playerAt(4, 4)
      .validateFrameTiming()
      .simulateFrames(10)
      .validateFrameTiming()
      .assertions.hasProcessedFrames(10)
      .assertions.systemsAreOperational()
  }

  test("UI state transition reliability") {
    // Tests critical UI transitions work correctly
    val potion = Given.items.potion("test-potion")
    
    scenarios.playerWithItems(4, 4, potion)
      .userInput.usesItem() // Enter item menu
      .userInput.cancels() // Exit without using
      .assertions.uiStateIs[UIState.Move.type] // Should return to move state
      .userInput.usesItem() // Re-enter item menu
      .userInput.confirms() // Use item this time
      .assertions.systemsAreOperational() // Should handle transitions correctly
  }

  test("Initiative and turn-based system integration") {
    // Validates that initiative system properly coordinates with other systems
    scenarios.playerAt(4, 4)
      .assertions.timerBasedSystemsWork()
      .userInput.movesUp()
      .waitUntilPlayerReady() // Player should become ready again
      .userInput.movesDown()
      .assertions.playerIsAt(4, 4) // Should be back at start
      .assertions.systemsAreOperational()
  }

  test("Rendering pipeline and game state synchronization") {
    // Ensures rendering pipeline stays synchronized with game state
    scenarios.playerAt(4, 4)
      .validateRenderableState()
      .userInput.movesUp()
      .validateRenderableState() // State should remain renderable after changes
      .userInput.movesDown()
      .validateRenderableState() // Should be consistent after multiple changes
  }

  test("System stability under moderate stress") {
    // Tests system stability with reasonable load
    scenarios.playerAt(4, 4)
      .simulateFrames(20) // Extended frame simulation
      .assertions.systemsAreOperational()
      .userInput.movesUp()
      .userInput.movesDown()
      .userInput.movesLeft()
      .userInput.movesRight()
      .assertions.playerIsAt(4, 4) // Should return to start after movement sequence
      .assertions.systemsAreOperational()
  }

  test("Combat system integration with core systems") {
    // Tests combat integration with movement, initiative, and health systems
    val enemy = Given.enemies.basic("integration-enemy", 5, 4, health = 3)
    
    scenarios.playerWithEnemies(4, 4, enemy*)
      .userInput.movesRight() // Move toward enemy
      .waitUntilPlayerReady()
      .userInput.primaryAttack() // Initiate combat
      .simulateFrames(5) // Allow combat to process
      .assertions.systemsAreOperational() // All systems should remain stable
  }

  test("Error recovery and edge case handling") {
    // Tests that the system handles edge cases gracefully
    scenarios.playerAt(4, 4)
      .userInput.usesItem() // Try to use item when none available
      .userInput.equipItems() // Try to equip when no equipment
      .userInput.interacts() // Try to interact when nothing to interact with
      .assertions.systemsAreOperational() // Should remain stable despite invalid actions
  }
}