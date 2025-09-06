package integration

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import testsupport.FullSystemTestFramework.*
import testsupport.Given
import ui.UIState

/**
 * Full System Framework Demonstration Test
 * 
 * This test demonstrates that the Full System Testing Framework successfully validates
 * the complete game pipeline from user input to game state changes, including areas
 * that were previously untested (indigoengine package integration).
 */
class FullSystemFrameworkDemonstrationTest extends AnyFunSuiteLike with Matchers {

  test("Full System Framework: Complete game functionality validation") {
    // Test 1: Basic movement pipeline (input mapping -> game state)
    val movementTest = scenarios.playerAt(10, 10)
      .userInput.movesUp()
      .assertions.playerIsAt(10, 9)
      .waitUntilPlayerReady()
      .userInput.movesRight()
      .assertions.playerIsAt(11, 9)
    
    // Test 2: Frame timing and performance
    val performanceTest = movementTest
      .validateFrameTiming() // Ensure frame processing is fast enough for real-time gameplay
      
    // Test 3: UI state management and transitions  
    val uiTest = performanceTest
      .assertions.uiStateIs[UIState.Move.type] // Validate UI state
      .assertions.systemsAreOperational() // Validate game systems integrity
      
    // Test 4: Initiative system integration (turn-based gameplay)
    val initiativeTest = uiTest
      .assertions.timerBasedSystemsWork() // Validate frame-based systems work correctly
      
    // Test 5: Rendering pipeline validation
    val renderingTest = initiativeTest
      .validateRenderableState() // Ensure game state is in a renderable condition
      
    // Test 6: Input variety (different key mappings work)
    val inputTest = renderingTest
      .waitUntilPlayerReady()
      .userInput.pressesKey(indigo.Key.KEY_W) // WASD movement
      .assertions.playerIsAt(11, 8)
      .waitUntilPlayerReady()
      .userInput.pressesKey(indigo.Key.ARROW_DOWN) // Arrow key movement
      .assertions.playerIsAt(11, 9)
      
    // Test 7: Error handling and system stability
    val stabilityTest = inputTest
      .simulateFrames(50) // Stress test with many frame updates
      .assertions.systemsAreOperational() // Should remain stable
      .assertions.hasProcessedFrames(50 + inputTest.currentFrame) // Frame counting works
      
    // Test 8: Complex multi-step workflow
    stabilityTest
      .waitUntilPlayerReady()
      .userInput.movesLeft()
      .waitUntilPlayerReady()
      .userInput.movesDown()
      .assertions.playerIsAt(10, 10) // Back to start position
      .assertions.systemsAreOperational() // Still operational after complex workflow
  }

  test("Full System Framework: Multi-entity interaction validation") {
    val enemy1 = Given.enemies.basic("demo-enemy-1", 12, 10, health = 5)
    val enemy2 = Given.enemies.basic("demo-enemy-2", 8, 10, health = 3)
    
    scenarios.playerWithEnemies(10, 10, (enemy1 ++ enemy2)*)
      .assertions.entityCountIs(6) // Player + 2 enemies + 4 weapons
      .userInput.movesRight() // Move toward enemy1
      .assertions.playerIsAt(11, 10)
      .waitUntilPlayerReady()
      .userInput.movesLeft() // Move back
      .assertions.playerIsAt(10, 10)
      .waitUntilPlayerReady()
      .userInput.movesLeft() // Move toward enemy2
      .assertions.playerIsAt(9, 10)
      .assertions.entityCountIs(6) // All entities still present
      .assertions.systemsAreOperational() // Systems handle multiple entities correctly
  }
  
  test("Full System Framework: Extended session simulation") {
    // Simulate a longer gameplay session to test system stability
    scenarios.playerAt(5, 5)
      .userInput.movesUp()
      .waitUntilPlayerReady()
      .userInput.movesRight()
      .waitUntilPlayerReady()
      .userInput.movesDown()
      .waitUntilPlayerReady()
      .userInput.movesLeft()
      .assertions.playerIsAt(5, 5) // Complete movement loop
      .simulateFrames(100) // Long time passage
      .assertions.systemsAreOperational() // Still stable after extended time
      .validateFrameTiming() // Performance still good
      .validateRenderableState() // Still renderable
      .assertions.hasProcessedFrames(100 + 4) // Correct frame count tracking
  }
}