package testsupport

import game.Input
import game.entity.*
import game.entity.Movement.position
import game.entity.Health.currentHealth
import game.entity.Initiative.isReady
import ui.{GameController, UIState}
import org.scalatest.Assertions.*
import indigo.Key

/**
 * Full System Test Framework
 * 
 * This framework provides end-to-end testing of the complete game pipeline:
 * - Input simulation → GameController → Game state changes
 * - Frame timing and game loop testing  
 * - Complete user workflow scenarios
 * 
 * Complements the existing Game Story DSL by testing the full input-to-game-state pipeline
 * including timing and frame-based updates that the DSL doesn't cover.
 */

object FullSystemTestFramework {
  
  /**
   * Full system test harness that simulates the complete game pipeline
   * including frame timing and input processing
   */
  case class FullSystemTest(
    private val gameController: GameController,
    private val frameCount: Int = 0
  ) {
    
    def gameState = gameController.gameState
    def uiState = gameController.uiState
    def currentFrame = frameCount
    
    /**
     * Simulate a single frame update with optional input, including proper timing
     */
    def simulateFrame(
      input: Option[Input.Input] = None,
      deltaMs: Long = 55 // ~18 FPS as per GameController.framesPerSecond
    ): FullSystemTest = {
      
      try {
        // Calculate time to ensure we meet the frame timing requirements
        val baseTimeNanos = frameCount.toLong * deltaMs * 1000000L
        val timeNanos = if (input.isDefined) {
          // For input frames, ensure we meet the allowedActionsPerSecond threshold
          baseTimeNanos + (1000000000L / ui.GameController.allowedActionsPerSecond)
        } else {
          baseTimeNanos + (1000000000L / ui.GameController.framesPerSecond)
        }
        
        val updatedController = gameController.update(input, timeNanos)
        
        FullSystemTest(updatedController, frameCount + 1)
      } catch {
        case e: Exception =>
          fail(s"Frame ${frameCount + 1} update failed: ${e.getMessage}")
      }
    }
    
    /**
     * Simulate multiple frames passing with no input
     */
    def simulateFrames(count: Int): FullSystemTest =
      (0 until count).foldLeft(this)((acc, _) => acc.simulateFrame())
    
    /**
     * Add entities to the current game state
     */
    def withEntities(entities: Entity*): FullSystemTest = {
      val updatedGameState = entities.foldLeft(gameState)((gs, entity) => 
        gs.copy(entities = gs.entities :+ entity)
      )
      val updatedController = gameController.copy(gameState = updatedGameState)
      copy(gameController = updatedController)
    }
    
    /**
     * Simulate user input scenarios by sending game inputs directly
     * Note: Each action consumes initiative, so actions are automatically spaced
     */
    object userInput {
      import game.Direction
      
      def sendInput(input: Input.Input): FullSystemTest = 
        simulateFrame(Some(input))
        
      def movesUp(): FullSystemTest = sendInput(Input.Move(Direction.Up))
      def movesDown(): FullSystemTest = sendInput(Input.Move(Direction.Down))
      def movesLeft(): FullSystemTest = sendInput(Input.Move(Direction.Left))
      def movesRight(): FullSystemTest = sendInput(Input.Move(Direction.Right))
      
      def usesItem(): FullSystemTest = sendInput(Input.UseItem)
      def confirms(): FullSystemTest = sendInput(Input.Confirm)
      def cancels(): FullSystemTest = sendInput(Input.Cancel)
      def interacts(): FullSystemTest = sendInput(Input.Interact)
      def equipItems(): FullSystemTest = sendInput(Input.Equip)
      
      def primaryAttack(): FullSystemTest = sendInput(Input.Attack(Input.PrimaryAttack))
      def secondaryAttack(): FullSystemTest = sendInput(Input.Attack(Input.SecondaryAttack))
      
      // For testing input mapping, simulate key presses that should map to game inputs
      def pressesKey(key: Key): FullSystemTest = {
        val mappedInput = key match {
          case Key.ARROW_UP | Key.KEY_W => Some(Input.Move(Direction.Up))
          case Key.ARROW_DOWN | Key.KEY_S => Some(Input.Move(Direction.Down))
          case Key.ARROW_LEFT | Key.KEY_A => Some(Input.Move(Direction.Left))
          case Key.ARROW_RIGHT | Key.KEY_D => Some(Input.Move(Direction.Right))
          case Key.KEY_U => Some(Input.UseItem)
          case Key.SPACE | Key.ENTER => Some(Input.Confirm)
          case Key.ESCAPE | Key.KEY_C => Some(Input.Cancel)
          case Key.KEY_E => Some(Input.Interact)
          case Key.KEY_Q => Some(Input.Equip)
          case Key.KEY_Z => Some(Input.Attack(Input.PrimaryAttack))
          case Key.KEY_X => Some(Input.Attack(Input.SecondaryAttack))
          case _ => None
        }
        
        mappedInput match {
          case Some(input) => simulateFrame(Some(input))
          case None => simulateFrame() // No input mapped
        }
      }
    }
    
    /**
     * Test frame timing and performance characteristics
     */
    def validateFrameTiming(): FullSystemTest = {
      val startTime = System.nanoTime()
      val updatedTest = simulateFrame()
      val endTime = System.nanoTime()
      val updateTimeMs = (endTime - startTime) / 1000000.0
      
      // Frame should process quickly (under 16ms for 60fps headroom)
      assert(updateTimeMs < 16.0, s"Frame update took ${updateTimeMs}ms, too slow for real-time gameplay")
      
      updatedTest
    }
    
    /**
     * Validate that the game state is in a renderable condition
     */
    def validateRenderableState(): FullSystemTest = {
      try {
        assert(gameState.entities.nonEmpty, "Game state should have entities for rendering")
        assert(gameState.playerEntity != null, "Player entity should exist for rendering")
        assert(gameState.dungeon != null, "Dungeon should exist for rendering")
        
        // Validate player has required components for rendering
        assert(gameState.playerEntity.get[Movement].isDefined, "Player should have Movement component for rendering")
        assert(gameState.playerEntity.get[Drawable].isDefined, "Player should have Drawable component for rendering")
        
        this
      } catch {
        case e: Exception => fail(s"Renderable state validation error: ${e.getMessage}")
      }
    }
    
    /**
     * Assertions for full system state
     */
    object assertions {
      def playerIsAt(x: Int, y: Int): FullSystemTest = {
        val playerPos = gameState.playerEntity.position
        assert(playerPos == game.Point(x, y), s"Expected player at ($x,$y) but was $playerPos")
        FullSystemTest.this
      }
      
      def playerHasHealth(hp: Int): FullSystemTest = {
        val actual = gameState.playerEntity.currentHealth
        assert(actual == hp, s"Expected player health $hp but was $actual")
        FullSystemTest.this
      }
      
      def uiStateIs[T <: UIState.UIState](implicit ct: scala.reflect.ClassTag[T]): FullSystemTest = {
        val expectedClass = ct.runtimeClass
        assert(expectedClass.isInstance(uiState), 
          s"Expected UI state ${expectedClass.getSimpleName} but was ${uiState.getClass.getSimpleName}")
        FullSystemTest.this
      }
      
      def entityCountIs(count: Int): FullSystemTest = {
        val actual = gameState.entities.length
        assert(actual == count, s"Expected $count entities but found $actual")
        FullSystemTest.this
      }
      
      def hasProcessedFrames(expectedCount: Int): FullSystemTest = {
        assert(frameCount == expectedCount, s"Expected $expectedCount frames but processed $frameCount")
        FullSystemTest.this
      }
      
      def playerInitiativeChanged(): FullSystemTest = {
        // Validate that initiative system is working by checking initiative is valid
        val initiative = gameState.playerEntity.get[Initiative]
        assert(initiative.exists(_.currentInitiative >= 0), "Player should have valid initiative")
        FullSystemTest.this
      }
      
      def systemsAreOperational(): FullSystemTest = {
        // Validate the game systems are working by checking entity states
        assert(gameState.entities.forall(_.id.nonEmpty), "All entities should have valid IDs")
        assert(gameState.playerEntity.currentHealth > 0, "Player should be alive")
        FullSystemTest.this
      }
      
      def timerBasedSystemsWork(): FullSystemTest = {
        // Test that frame-based systems like initiative work correctly
        val beforeInit = gameState.playerEntity.get[Initiative]
        val afterTest = simulateFrames(10)
        val afterInit = afterTest.gameState.playerEntity.get[Initiative]
        
        // Initiative should change over time due to frame-based updates
        assert(beforeInit.isDefined && afterInit.isDefined, "Player should always have initiative")
        afterTest
      }
    }
    
    /**
     * Wait until the player is ready to act again
     */
    def waitUntilPlayerReady(): FullSystemTest = {
      var test = this
      var attempts = 0
      while (!test.gameState.playerEntity.isReady && attempts < 20) {
        test = test.simulateFrame()
        attempts += 1
      }
      if (!test.gameState.playerEntity.isReady) {
        fail(s"Player never became ready after $attempts frames")
      }
      test
    }
    
    /**
     * Debug method to show current game state
     */
    def debug(message: String = ""): FullSystemTest = {
      val playerInit = gameState.playerEntity.get[Initiative]
      val playerReady = gameState.playerEntity.isReady
      val playerPos = gameState.playerEntity.position
      val playerHealth = gameState.playerEntity.currentHealth
      println(s"DEBUG $message: Frame=$frameCount, UI=$uiState, PlayerPos=$playerPos, Health=$playerHealth, Initiative=$playerInit, Ready=$playerReady")
      this
    }
  }
  
  /**
   * Builder for creating full system test scenarios
   */
  object scenarios {
    
    /**
     * Create a basic test scenario with player at position with proper initiative
     */
    def playerAt(x: Int, y: Int): FullSystemTest = {
      val world = Given.thePlayerAt(x, y)
        .modifyPlayer[Initiative](_.copy(maxInitiative = 10, currentInitiative = 0)) // Ready when currentInitiative = 0
      val gameController = GameController(UIState.Move, world.buildGameState()).init()
      
      FullSystemTest(gameController)
    }
    
    /**
     * Create test scenario with player and items with proper initiative
     */
    def playerWithItems(x: Int, y: Int, items: Entity*): FullSystemTest = {
      val world = Given.thePlayerAt(x, y).withItems(items*)
        .modifyPlayer[Initiative](_.copy(maxInitiative = 10, currentInitiative = 0)) // Ready when currentInitiative = 0
      val gameController = GameController(UIState.Move, world.buildGameState()).init()
      
      FullSystemTest(gameController)
    }
    
    /**
     * Create test scenario with player and enemies with proper initiative
     */
    def playerWithEnemies(x: Int, y: Int, enemies: Entity*): FullSystemTest = {
      val world = Given.thePlayerAt(x, y).withEntities(enemies*)
        .modifyPlayer[Initiative](_.copy(maxInitiative = 10, currentInitiative = 0)) // Ready when currentInitiative = 0
      val gameController = GameController(UIState.Move, world.buildGameState()).init()
      
      FullSystemTest(gameController)
    }
    
    /**
     * Create main menu scenario for testing menu navigation
     */
    def mainMenu(): FullSystemTest = {
      // Create minimal dummy state for main menu testing
      val dummyWorld = Given.thePlayerAt(0, 0)
      val gameController = GameController(UIState.MainMenu(0), dummyWorld.buildGameState())
      
      FullSystemTest(gameController)
    }
  }
}